package no.nav.helse.flex.testdata

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("test", "testdatareset")
class TestdataResetListener {
    val log = logger()

    @WithSpan
    @KafkaListener(
        topics = [TESTDATA_RESET_TOPIC],
        id = "flex-sykmeldinger-backend-testdatareset-v1",
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val fnr = cr.value()
        log.info("Mottatt testdata reset for: $fnr")
        acknowledgment.acknowledge()
    }
}

const val TESTDATA_RESET_TOPIC = "flex.testdata-reset"
