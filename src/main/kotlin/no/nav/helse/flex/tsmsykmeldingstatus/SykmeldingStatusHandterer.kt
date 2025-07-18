package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.producers.KafkaMetadataDTO
import no.nav.helse.flex.producers.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.producers.SykmeldingStatusProducer
import no.nav.helse.flex.sykmelding.SykmeldingHendelseException
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.tsmsykmeldingstatus.dto.StatusEventKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

const val SYKMELDINGSTATUS_TOPIC: String = "teamsykmelding.sykmeldingstatus-leesah"
const val SYKMELDINGSTATUS_LEESAH_SOURCE = "flex-sykmeldinger-backend"

@Service
class SykmeldingStatusHandterer(
    private val sykmeldingHendelseFraKafkaKonverterer: SykmeldingHendelseFraKafkaKonverterer,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusProducer: SykmeldingStatusProducer,
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
                    log.debug(
                        "Ignorerer status $statusEvent for sykmelding '$sykmeldingId'. Sykmelding slettes kun ved tombstone på sykmeldinger topic",
                    )
                    true
                }
                StatusEventKafkaDTO.APEN -> {
                    log.debug("Ignorerer status $statusEvent for sykmelding '$sykmeldingId'")
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

    fun sendSykmeldingStatusPaKafka(sykmelding: Sykmelding) {
        val status =
            sammenstillSykmeldingStatusKafkaMessageDTO(
                fnr = sykmelding.pasientFnr,
                sykmeldingStatusKafkaDTO =
                    SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                        sykmeldingHendelse = sykmelding.sisteHendelse(),
                        sykmeldingId = sykmelding.sykmeldingId,
                    ),
            )
        sykmeldingStatusProducer.produserSykmeldingStatus(status)
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

        if (hendelse.hendelseOpprettet.isAfter(Instant.parse("2025-07-01T00:00:00Z"))) {
            if (finnesDuplikatHendelsePaaSykmelding(sykmelding, hendelse)) {
                log.warn(
                    "Hendelse ${hendelse.status} for sykmelding $sykmeldingId eksisterer allerede, " +
                        "hopper over lagring av hendelse",
                )
                return false
            }

            validerStatusForSykmelding(sykmelding, status)
        }
        sykmeldingRepository.save(sykmelding.leggTilHendelse(hendelse))
        log.info("Hendelse ${hendelse.status} for sykmelding $sykmeldingId lagret")

        return true
    }

    private fun validerStatusForSykmelding(
        sykmelding: Sykmelding,
        status: SykmeldingStatusKafkaMessageDTO,
    ) {
        if (sykmelding.sisteHendelse().status == HendelseStatus.APEN) {
            return
        }
        val statusFraKafkaOpprettet = status.event.timestamp.toInstant()
        if (sykmelding.sisteHendelse().hendelseOpprettet.isAfter(statusFraKafkaOpprettet)) {
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

        internal fun sammenstillSykmeldingStatusKafkaMessageDTO(
            fnr: String,
            sykmeldingStatusKafkaDTO: SykmeldingStatusKafkaDTO,
        ): SykmeldingStatusKafkaMessageDTO {
            val sykmeldingId = sykmeldingStatusKafkaDTO.sykmeldingId
            val metadataDTO =
                KafkaMetadataDTO(
                    sykmeldingId = sykmeldingId,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                    fnr = fnr,
                    source = SYKMELDINGSTATUS_LEESAH_SOURCE,
                )

            return SykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = metadataDTO,
                event = sykmeldingStatusKafkaDTO,
            )
        }

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
                tidsDifferanse <= 1 &&
                hendelse1.brukerSvar == hendelse2.brukerSvar &&
                hendelse1.tilleggsinfo == hendelse2.tilleggsinfo
        }
    }
}
