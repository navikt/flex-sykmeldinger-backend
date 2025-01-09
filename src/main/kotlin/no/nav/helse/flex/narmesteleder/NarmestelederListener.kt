package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class NarmestelederListener(
    private val oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder,
) : ConsumerSeekAware {
    val log = logger()

    @KafkaListener(
        topics = [NARMESTELEDER_LEESAH_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        oppdateringAvNarmesteLeder.behandleMeldingFraKafka(cr.value())
        acknowledgment.acknowledge()
    }

    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long>,
        callback: ConsumerSeekAware.ConsumerSeekCallback,
    ) {
        log.info("Partitions assigned, seeking to beginning")
        assignments.keys.forEach { topicPartition ->
            callback.seekToBeginning(topicPartition.topic(), topicPartition.partition())
        }
    }
}

const val NARMESTELEDER_LEESAH_TOPIC = "teamsykmelding.syfo-narmesteleder-leesah"
