package no.nav.helse.flex.narmesteleder

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("test")
class NarmestelederListener(
    private val oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder,
) {
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
}

const val NARMESTELEDER_LEESAH_TOPIC = "teamsykmelding.syfo-narmesteleder-leesah"
