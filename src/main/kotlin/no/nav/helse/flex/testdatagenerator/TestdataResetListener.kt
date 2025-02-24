package no.nav.helse.flex.testdatagenerator

import no.nav.helse.flex.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("testdata")
class TestdataResetListener {
    val log = logger()

    @KafkaListener(
        topics = [TESTDATA_RESET_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        // val fnr = cr.value()

        log.info("Mottatt reset melding. Gjør per nå ingenting")
        acknowledgment.acknowledge()
    }
}

const val TESTDATA_RESET_TOPIC = "flex.testdata-reset"
