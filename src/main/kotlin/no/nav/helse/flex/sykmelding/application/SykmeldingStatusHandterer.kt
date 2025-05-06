package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.producers.sykmeldingstatus.*
import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatusEndrer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class SykmeldingStatusHandterer(
    private val sykmeldingHendelseKonverterer: SykmeldingHendelseKonverterer,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusEndrer: SykmeldingStatusEndrer,
    private val sykmeldingStatusProducer: SykmeldingStatusProducer,
) {
    private val log = logger()

    fun handterSykmeldingStatus(status: SykmeldingStatusKafkaMessageDTO): Boolean {
        if (status.erFraEgetSystem()) {
            log.info("Hendelse er fra flex-sykmeldinger-backend, ignorerer")
            return false
        }

        val sykmelding =
            sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
                ?: return handterManglendesSykmelding(status)

        return behandleStatusForEksisterendeSykmelding(sykmelding, status)
    }

    private fun SykmeldingStatusKafkaMessageDTO.erFraEgetSystem(): Boolean = this.kafkaMetadata.source == SYKMELDINGSTATUS_LEESAH_SOURCE

    private fun handterManglendesSykmelding(status: SykmeldingStatusKafkaMessageDTO): Boolean {
        val sykmeldingId = status.kafkaMetadata.sykmeldingId
        log.info(
            "Fant ikke sykmelding med id $sykmeldingId, " +
                "publiserer hendelse på retry topic",
        )
        publiserPaRetryTopic(status)
        return false
    }

    private fun behandleStatusForEksisterendeSykmelding(
        sykmelding: Sykmelding,
        status: SykmeldingStatusKafkaMessageDTO,
    ): Boolean {
        val hendelse = sykmeldingHendelseKonverterer.konverterStatusTilSykmeldingHendelse(sykmelding, status)
        val sykmeldingId = sykmelding.sykmeldingId

        if (hendelseEksistererPaaSykmelding(sykmelding, hendelse)) {
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

    fun hendelseEksistererPaaSykmelding(
        sykmelding: Sykmelding,
        sykmeldingHendelse: SykmeldingHendelse,
    ): Boolean = sykmelding.hendelser.any { erHendelseDuplikat(sykmeldingHendelse, it) }

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

    private fun publiserPaRetryTopic(hendelse: SykmeldingStatusKafkaMessageDTO) {
        // TODO: Implementer retry-mekanisme
        log.warn("Retry topic er ikke implementert")
    }

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

    companion object {
        fun erHendelseDuplikat(
            hendelse1: SykmeldingHendelse,
            hendelse2: SykmeldingHendelse,
        ): Boolean =
            hendelse1.status == hendelse2.status &&
                hendelse1.opprettet == hendelse2.opprettet &&
                hendelse1.arbeidstakerInfo == hendelse2.arbeidstakerInfo &&
                hendelse1.brukerSvar == hendelse2.brukerSvar &&
                hendelse1.tilleggsinfo == hendelse2.tilleggsinfo
    }
}

const val SYKMELDINGSTATUS_TOPIC: String = "teamsykmelding.sykmeldingstatus-leesah"
const val SYKMELDINGSTATUS_LEESAH_SOURCE = "flex-sykmeldinger-backend"
