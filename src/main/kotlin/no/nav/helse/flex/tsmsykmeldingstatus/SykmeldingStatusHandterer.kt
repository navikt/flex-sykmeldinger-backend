package no.nav.helse.flex.tsmsykmeldingstatus

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
                    validerStatusForSykmelding(sykmelding, status)
                    return lagreStatusForEksisterendeSykmelding(sykmelding, status)
                }
            }
        }
    }

    fun validerStatusForSykmelding(
        sykmelding: Sykmelding,
        status: SykmeldingStatusKafkaMessageDTO,
    ) {
        if (sykmelding.sisteHendelse().status == HendelseStatus.APEN) {
            return
        }
        val statusFraKafkaOpprettet = status.event.timestamp.toInstant()
        if (sykmelding.sisteHendelse().hendelseOpprettet.isAfter(statusFraKafkaOpprettet)) {
            log.error(
                "SykmeldingId: ${sykmelding.sykmeldingId} har en hendelse som er nyere enn statusen som kom fra kafka. " +
                    "Hendelse: ${sykmelding.sisteHendelse().hendelseOpprettet}, status: $statusFraKafkaOpprettet",
            )
            throw SykmeldingHendelseException(
                "SykmeldingId: ${sykmelding.sykmeldingId} har en hendelse som er nyere enn statusen som kom fra kafka",
            )
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
                        sykmeldingId = sykmelding.sykmeldingId,
                        sykmeldingHendelse = sykmelding.sisteHendelse(),
                    ),
            )
        sykmeldingStatusProducer.produserSykmeldingStatus(status)
    }

    private fun lagreStatusForEksisterendeSykmelding(
        sykmelding: Sykmelding,
        status: SykmeldingStatusKafkaMessageDTO,
    ): Boolean {
        val hendelse =
            try {
                sykmeldingHendelseFraKafkaKonverterer.konverterSykmeldingHendelseFraKafkaDTO(sykmelding, status)
            } catch (e: Exception) {
                log.errorSecure(
                    "Feil ved konvertering av sykmeldingstatus fra kafka, status: ${status.event.statusEvent}, " +
                        "sykmeldingId: ${status.kafkaMetadata.sykmeldingId}",
                    secureMessage = "Sykmeldingstatus: $status",
                    secureThrowable = e,
                )
                throw e
            }
        val sykmeldingId = sykmelding.sykmeldingId

        if (finnesDuplikatHendelsePaaSykmelding(sykmelding, hendelse)) {
            log.warn(
                "Hendelse ${hendelse.status} for sykmelding $sykmeldingId eksisterer allerede, " +
                    "hopper over lagring av hendelse",
            )
            return false
        } else {
            log.info(
                "Håndterer hendelse ${hendelse.status} for sykmelding $sykmeldingId, " +
                    "fra source ${status.kafkaMetadata.source}",
            )
        }

        sykmeldingRepository.save(sykmelding.leggTilHendelse(hendelse))
        log.info("Hendelse ${hendelse.status} for sykmelding $sykmeldingId lagret")

        return true
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
        ): Boolean =
            hendelse1.status == hendelse2.status &&
                hendelse1.hendelseOpprettet == hendelse2.hendelseOpprettet &&
                hendelse1.brukerSvar == hendelse2.brukerSvar &&
                hendelse1.tilleggsinfo == hendelse2.tilleggsinfo
    }
}
