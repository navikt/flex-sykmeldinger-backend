package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

interface SykmeldingStatusProducer {
    fun produserSykmeldingStatus(
        fnr: String,
        sykmelingstatusDTO: SykmeldingStatusKafkaDTO,
    ): Boolean
}

const val SYKMELDINGSTATUS_TOPIC: String = "teamsykmelding.sykmeldingstatus-leesah"
const val STATUS_LEESAH_SOURCE = "flex-sykmeldinger-backend"

@Component
class SykmeldingStatusKafkaProducer(
    private val meldingProducer: Producer<String, String>,
    private val environmentToggles: EnvironmentToggles,
) : SykmeldingStatusProducer {
    private val logger = logger()

    override fun produserSykmeldingStatus(
        fnr: String,
        sykmelingstatusDTO: SykmeldingStatusKafkaDTO,
    ): Boolean {
        if (environmentToggles.isProduction()) {
            return false
        }
        val sykmeldingId = sykmelingstatusDTO.sykmeldingId
        logger.info(
            "Skriver statusendring ${sykmelingstatusDTO.statusEvent} for sykmelding med id $sykmeldingId til topic på aiven",
        )
        val metadataDTO =
            KafkaMetadataDTO(
                sykmeldingId = sykmeldingId,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                fnr = fnr,
                source = STATUS_LEESAH_SOURCE,
            )
        val sykmeldingStatusKafkaMessageDTO =
            SykmeldingStatusKafkaMessageDTO(metadataDTO, sykmelingstatusDTO)
        try {
            meldingProducer
                .send(
                    ProducerRecord(
                        SYKMELDINGSTATUS_TOPIC,
                        sykmeldingId,
                        sykmeldingStatusKafkaMessageDTO.serialisertTilString(),
                    ),
                ).get()
            return true
        } catch (ex: Exception) {
            logger.error(
                "Failed to send sykmeldingStatus to kafkatopic, sykmelding: $sykmeldingId",
            )
            throw ex
        }
    }
}

data class SykmeldingStatusKafkaMessageDTO(
    val kafkaMetadata: KafkaMetadataDTO,
    val event: SykmeldingStatusKafkaDTO,
)

data class KafkaMetadataDTO(
    val sykmeldingId: String,
    val timestamp: OffsetDateTime,
    val fnr: String,
    val source: String,
)
