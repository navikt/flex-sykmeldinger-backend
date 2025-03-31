package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.producers.sykmeldingstatus.KafkaMetadataDTO
import no.nav.helse.flex.producers.sykmeldingstatus.STATUS_LEESAH_SOURCE
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatusEndrer
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class StatusHandterer(
    private val sykmeldingHendelseKonverterer: SykmeldingHendelseKonverterer,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val sykmeldingStatusEndrer: SykmeldingStatusEndrer,
) {
    private val log = logger()

    fun handterStatus(status: SykmeldingStatusKafkaMessageDTO): Boolean {
        val hendelse: SykmeldingHendelse = sykmeldingHendelseKonverterer.konverterStatusTilSykmeldingHendelse(status)
        log.info(
            "Håndterer hendelse ${hendelse.status} for sykmelding ${status.kafkaMetadata.sykmeldingId}, " +
                "fra source ${status.kafkaMetadata.source}",
        )
        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        if (sykmelding != null) {
            sykmeldingStatusEndrer.sjekkStatusEndring(sykmelding = sykmelding, nyStatus = hendelse.status)
            sykmeldingRepository.save(sykmelding.leggTilHendelse(hendelse))
        } else {
            publiserPaRetryTopic(hendelse).also {
                log.info(
                    "Fant ikke sykmelding med id ${status.kafkaMetadata.sykmeldingId}, " +
                        "publiserer hendelse på retry topic",
                )
                return false
            }
        }
        log.info("Hendelse ${hendelse.status} for sykmelding ${status.kafkaMetadata.sykmeldingId} lagret")
        return true
    }

    fun publiserPaRetryTopic(hendelse: SykmeldingHendelse) {
        // TODO
    }

    fun sammenstillSykmeldingStatusKafkaMessageDTO(
        fnr: String,
        sykmeldingStatusKafkaDTO: SykmeldingStatusKafkaDTO,
    ): SykmeldingStatusKafkaMessageDTO {
        val sykmeldingId = sykmeldingStatusKafkaDTO.sykmeldingId
        val metadataDTO =
            KafkaMetadataDTO(
                sykmeldingId = sykmeldingId,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                fnr = fnr,
                source = STATUS_LEESAH_SOURCE,
            )
        return SykmeldingStatusKafkaMessageDTO(kafkaMetadata = metadataDTO, event = sykmeldingStatusKafkaDTO).also {
            log.info("Sender statusendring ${it.serialisertTilString()}")
        }
    }
}
