package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.producers.sykmeldingstatus.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.sykmelding.application.StatusHandterer
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class StatusListener(
    private val statusHandterer: StatusHandterer,
) {
    @KafkaListener(
        topics = [SYKMELDINGSTATUS_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        // TODO: Hvordan offset?
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val status: SykmeldingStatusKafkaMessageDTO = objectMapper.readValue(cr.value())
        statusHandterer.handterStatus(status)
        acknowledgment.acknowledge()
    }
}
