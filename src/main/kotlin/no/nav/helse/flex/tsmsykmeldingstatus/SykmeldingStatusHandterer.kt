package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.gateways.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.gateways.aareg.AaregClient
import no.nav.helse.flex.sykmelding.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SYKMELDINGSTATUS_LEESAH_SOURCE
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelseException
import no.nav.helse.flex.tsmsykmeldingstatus.dto.StatusEventKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingStatusHandterer(
    private val sykmeldingHendelseFraKafkaKonverterer: SykmeldingHendelseFraKafkaKonverterer,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusBuffer: SykmeldingStatusBuffer,
    private val aaregClient: AaregClient,
) {
    private val log = logger()

    @Transactional(rollbackFor = [Exception::class])
    fun handterSykmeldingStatus(status: SykmeldingStatusKafkaMessageDTO): Boolean {
        if (status.erFraEgetSystem()) {
            log.info("Hendelse er fra flex-sykmeldinger-backend, ignorerer")
            return false
        }
        val sykmeldingId = status.kafkaMetadata.sykmeldingId
        sykmeldingStatusBuffer.taLaasFor(sykmeldingId)
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            log.info("Fant ikke sykmelding med id $sykmeldingId, buffrer status: ${status.event.statusEvent}")
            sykmeldingStatusBuffer.leggTil(status)
            return false
        } else {
            return when (val statusEvent = status.event.statusEvent) {
                StatusEventKafkaDTO.SLETTET -> {
                    log.info(
                        "Ignorerer status $statusEvent for sykmelding '$sykmeldingId'. Sykmelding slettes kun ved tombstone på sykmeldinger topic",
                    )
                    true
                }
                StatusEventKafkaDTO.APEN -> {
                    log.info("Ignorerer status $statusEvent for sykmelding '$sykmeldingId'")
                    true
                }
                else -> {
                    return lagreStatusForEksisterendeSykmelding(sykmelding, status)
                }
            }
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun prosesserSykmeldingStatuserFraBuffer(sykmeldingId: String) {
        sykmeldingStatusBuffer.taLaasFor(sykmeldingId)
        val buffredeStatuser = sykmeldingStatusBuffer.fjernAlleFor(sykmeldingId)
        for (status in buffredeStatuser) {
            handterSykmeldingStatus(status)
        }
    }

    private fun lagreStatusForEksisterendeSykmelding(
        sykmelding: Sykmelding,
        status: SykmeldingStatusKafkaMessageDTO,
    ): Boolean {
        log.info(
            "Håndterer hendelse ${status.event.statusEvent} for sykmelding ${sykmelding.sykmeldingId}, " +
                "fra source ${status.kafkaMetadata.source}",
        )

        val statusEvent =
            korrigerManglendeJuridiskOrgnummer(
                statusEvent = status.event,
                fnr = sykmelding.pasientFnr,
            )

        val hendelse =
            try {
                sykmeldingHendelseFraKafkaKonverterer.konverterSykmeldingHendelseFraKafkaDTO(
                    status = statusEvent,
                    erSykmeldingAvvist = sykmelding.erAvvist,
                    source = status.kafkaMetadata.source,
                )
            } catch (e: Exception) {
                log.errorSecure(
                    "Feil ved konvertering av sykmeldingstatus fra kafka, status: ${statusEvent.statusEvent}, " +
                        "sykmeldingId: ${status.kafkaMetadata.sykmeldingId}",
                    secureMessage = "Sykmeldingstatus: $status",
                    secureThrowable = e,
                )
                throw e
            }
        val sykmeldingId = sykmelding.sykmeldingId

        if (finnesDuplikatHendelsePaaSykmelding(sykmelding, hendelse)) {
            log.info(
                "Hendelse ${hendelse.status} for sykmelding $sykmeldingId eksisterer allerede, " +
                    "hopper over lagring av hendelse",
            )
            return false
        }

        validerStatusForSykmelding(sykmelding, status)
        sykmeldingRepository.save(sykmelding.leggTilHendelse(hendelse))
        log.info("Lagret hendelse ${hendelse.status} for sykmelding $sykmeldingId")

        return true
    }

    private fun validerStatusForSykmelding(
        sykmelding: Sykmelding,
        status: SykmeldingStatusKafkaMessageDTO,
    ) {
        if (sykmelding.sisteHendelse().status == HendelseStatus.APEN) {
            return
        }
        val sisteHendelse = sykmelding.sisteHendelse()

        val erNyStatusFraSammeSystem = sisteHendelse.source == status.kafkaMetadata.source
        if (erNyStatusFraSammeSystem) {
            return
        }

        val statusFraKafkaOpprettet = status.event.timestamp.toInstant()
        if (sisteHendelse.hendelseOpprettet.isAfter(statusFraKafkaOpprettet)) {
            log.errorSecure(
                message =
                    "SykmeldingId: ${sykmelding.sykmeldingId} har en hendelse som er nyere enn statusen som kom fra kafka. " +
                        "Hendelse: ${sykmelding.sisteHendelse().hendelseOpprettet}, status: $statusFraKafkaOpprettet",
                secureMessage = "Sykmeldingstatus: $status",
            )
            throw SykmeldingHendelseException(
                "SykmeldingId: ${sykmelding.sykmeldingId} har en hendelse som er nyere enn statusen som kom fra kafka",
            )
        }
    }

    private fun korrigerManglendeJuridiskOrgnummer(
        statusEvent: SykmeldingStatusKafkaDTO,
        fnr: String,
    ): SykmeldingStatusKafkaDTO {
        val arbeidsgiver = statusEvent.arbeidsgiver
        if (arbeidsgiver == null || arbeidsgiver.juridiskOrgnummer != null) {
            return statusEvent
        }
        val alleArbeidsforhold = aaregClient.getArbeidsforholdoversikt(fnr = fnr)
        val relatertArbeidsforhold =
            alleArbeidsforhold.arbeidsforholdoversikter.find {
                it.arbeidssted.finnOrgnummer() == arbeidsgiver.orgnummer
            }
        val juridiskOrgnummer =
            when (relatertArbeidsforhold) {
                null -> arbeidsgiver.orgnummer
                else -> relatertArbeidsforhold.opplysningspliktig.finnOrgnummer()
            }
        return statusEvent.copy(
            arbeidsgiver = arbeidsgiver.copy(juridiskOrgnummer = juridiskOrgnummer),
        )
    }

    companion object {
        private fun SykmeldingStatusKafkaMessageDTO.erFraEgetSystem(): Boolean = this.kafkaMetadata.source == SYKMELDINGSTATUS_LEESAH_SOURCE

        fun finnesDuplikatHendelsePaaSykmelding(
            sykmelding: Sykmelding,
            sykmeldingHendelse: SykmeldingHendelse,
        ): Boolean = sykmelding.hendelser.any { erHendelseDuplikat(sykmeldingHendelse, it) }

        fun erHendelseDuplikat(
            hendelse1: SykmeldingHendelse,
            hendelse2: SykmeldingHendelse,
        ): Boolean {
            val tidsDifferanse =
                kotlin.math.abs(
                    hendelse1.hendelseOpprettet.epochSecond - hendelse2.hendelseOpprettet.epochSecond,
                )

            return hendelse1.status == hendelse2.status &&
                tidsDifferanse <= 1
        }
    }
}
