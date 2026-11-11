package no.nav.helse.flex.gateways

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.narmesteleder.OppdateringAvNarmesteLederEsyfo
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class NarmestelederEsyfoListener(
    private val oppdateringAvNarmesteLederEsyfo: OppdateringAvNarmesteLederEsyfo,
) {
    @WithSpan
    @KafkaListener(
        topics = [NARMESTELEDER_ESYFO_LEESAH_TOPIC],
        containerFactory = "narmesteLederEsyfoKafkaListenerContainerFactory",
        autoStartup = "\${spring.kafka.listener.auto-startup:true}",
    )
    fun listenEsyfo(
        records: List<ConsumerRecord<String, String?>>,
        acknowledgment: Acknowledgment,
    ) {
        if (records.isEmpty()) return
        try {
            oppdateringAvNarmesteLederEsyfo.behandleBatch(records)
            acknowledgment.acknowledge()
        } catch (_: Exception) {
            throw KafkaErrorHandlerException(
                message = "ESYFO-projeksjon feilet; batchen blir forsøkt igjen",
                errorHandlerLoggingEnabled = false,
            )
        }
    }
}
