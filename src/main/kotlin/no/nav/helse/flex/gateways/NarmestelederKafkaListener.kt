package no.nav.helse.flex.gateways

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.narmesteleder.OppdateringAvNarmesteLeder
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class NarmestelederListener(
    private val oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder,
) {
    private val log = logger()

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
            log.errorSecure(
                message = "Feil ved håndtering av nærmeste leder hendelse, exception: ${e::class.simpleName}. Dette vil bli retryet",
                secureThrowable = e,
            )
            throw KafkaErrorHandlerException(
                errorHandlerLoggingEnabled = false,
                cause = e,
            )
        }
        acknowledgment.acknowledge()
    }
}

const val NARMESTELEDER_LEESAH_TOPIC = "teamsykmelding.syfo-narmesteleder-leesah"
