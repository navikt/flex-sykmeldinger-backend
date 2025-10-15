package no.nav.helse.flex.gateways

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.narmesteleder.OppdateringAvNarmesteLeder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class NarmestelederListener(
    private val oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder,
) {
    @WithSpan
    @KafkaListener(
        topics = [NARMESTELEDER_LEESAH_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        // TODO: Hvordan offset?
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            oppdateringAvNarmesteLeder.behandleMeldingFraKafka(cr.value())
        } catch (e: Exception) {
            throw KafkaErrorHandlerException(
                cause = e,
                insensitiveMessage = "Feil ved håndtering av nærmeste leder hendelse",
            )
        }
        acknowledgment.acknowledge()
    }
}

const val NARMESTELEDER_LEESAH_TOPIC = "teamsykmelding.syfo-narmesteleder-leesah"
