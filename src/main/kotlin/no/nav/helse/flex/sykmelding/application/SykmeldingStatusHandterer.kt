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

    // TODO
    private fun publiserPaRetryTopic(hendelse: SykmeldingHendelse) {
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
        return SykmeldingStatusKafkaMessageDTO(kafkaMetadata = metadataDTO, event = sykmeldingStatusKafkaDTO)
    }
}

const val SYKMELDINGSTATUS_TOPIC: String = "teamsykmelding.sykmeldingstatus-leesah"
const val SYKMELDINGSTATUS_LEESAH_SOURCE = "flex-sykmeldinger-backend"
