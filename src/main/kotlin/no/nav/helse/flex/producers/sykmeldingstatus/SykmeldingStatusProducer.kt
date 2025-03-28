package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

interface SykmeldingStatusProducer {
    fun produserSykmeldingStatus(sykmeldingStatusKafkaMessageDTO: SykmeldingStatusKafkaMessageDTO): Boolean
}

const val SYKMELDINGSTATUS_TOPIC: String = "teamsykmelding.sykmeldingstatus-leesah"
const val STATUS_LEESAH_SOURCE = "flex-sykmeldinger-backend"

@Component
class SykmeldingStatusKafkaProducer(
    private val meldingProducer: Producer<String, String>,
    private val environmentToggles: EnvironmentToggles,
) : SykmeldingStatusProducer {
    private val log = logger()

    override fun produserSykmeldingStatus(sykmeldingStatusKafkaMessageDTO: SykmeldingStatusKafkaMessageDTO): Boolean {
        if (environmentToggles.isProduction()) {
            return false
        }
        log.info(
            "Skriver statusendring ${sykmeldingStatusKafkaMessageDTO.event.statusEvent} " +
                "for sykmelding med id ${sykmeldingStatusKafkaMessageDTO.event.sykmeldingId} til topic $SYKMELDINGSTATUS_TOPIC",
        )
        try {
            meldingProducer
                .send(
                    ProducerRecord(
                        SYKMELDINGSTATUS_TOPIC,
                        sykmeldingStatusKafkaMessageDTO.event.sykmeldingId,
                        sykmeldingStatusKafkaMessageDTO.serialisertTilString(),
                    ),
                ).get()
            return true
        } catch (ex: Exception) {
            log.error(
                "Failed to send sykmeldingStatus to kafkatopic, sykmelding: ${sykmeldingStatusKafkaMessageDTO.event.sykmeldingId}",
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
