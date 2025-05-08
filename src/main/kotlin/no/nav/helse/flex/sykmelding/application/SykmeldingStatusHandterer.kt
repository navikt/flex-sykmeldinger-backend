package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.producers.sykmeldingstatus.*
import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatusEndrer
import no.nav.helse.flex.sykmeldingstatusbuffer.SykmeldingStatusBuffer
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
    private val sykmeldingHendelseKonverterer: SykmeldingHendelseKonverterer,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusEndrer: SykmeldingStatusEndrer,
    private val sykmeldingStatusProducer: SykmeldingStatusProducer,
    private val sykmeldingStatusBuffer: SykmeldingStatusBuffer,
) {
    private val log = logger()

    fun lagreSykmeldingStatus(status: SykmeldingStatusKafkaMessageDTO): Boolean {
        if (status.erFraEgetSystem()) {
            log.info("Hendelse er fra flex-sykmeldinger-backend, ignorerer")
            return false
        }

        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        if (sykmelding == null) {
            leggStatusIBuffer(status)
            return false
        } else {
            return lagreStatusForEksisterendeSykmelding(sykmelding, status)
        }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun prosesserSykmeldingStatuserFraBuffer(sykmeldingId: String) {
        val buffredeStatuser = sykmeldingStatusBuffer.prosesserAlleFor(sykmeldingId)
        for (status in buffredeStatuser) {
            lagreSykmeldingStatus(status)
        }
    }

    fun sendSykmeldingStatusPaKafka(sykmelding: Sykmelding) {
        val status =
            sammenstillSykmeldingStatusKafkaMessageDTO(
                fnr = sykmelding.pasientFnr,
                sykmeldingStatusKafkaDTO =
                    SykmeldingStatusKafkaDTOKonverterer.fraSykmeldingHendelse(
                        sykmeldingId = sykmelding.sykmeldingId,
                        sykmeldingHendelse = sykmelding.sisteHendelse(),
                    ),
            )
        sykmeldingStatusProducer.produserSykmeldingStatus(status)
    }

    private fun leggStatusIBuffer(status: SykmeldingStatusKafkaMessageDTO) {
        val sykmeldingId = status.kafkaMetadata.sykmeldingId
        val statusEvent = status.event.statusEvent
        log.info(
            "Fant ikke sykmelding med id $sykmeldingId, " +
                "buffrer status: $statusEvent",
        )
        sykmeldingStatusBuffer.leggTil(status)
    }

    private fun lagreStatusForEksisterendeSykmelding(
        sykmelding: Sykmelding,
        status: SykmeldingStatusKafkaMessageDTO,
    ): Boolean {
        val hendelse =
            try {
                sykmeldingHendelseKonverterer.konverterStatusTilSykmeldingHendelse(sykmelding, status)
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

        sykmeldingStatusEndrer.sjekkStatusEndring(sykmelding = sykmelding, nyStatus = hendelse.status)
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
                hendelse1.opprettet == hendelse2.opprettet &&
                hendelse1.brukerSvar == hendelse2.brukerSvar &&
                hendelse1.tilleggsinfo == hendelse2.tilleggsinfo
    }
}
