package no.nav.helse.flex.gateways

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.narmesteleder.OppdateringAvNarmesteLeder
import no.nav.helse.flex.narmesteleder.OppdateringAvNarmesteLederEsyfo
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class NarmestelederListener(
    private val oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder,
    private val oppdateringAvNarmesteLederEsyfo: OppdateringAvNarmesteLederEsyfo,
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
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) = behandle(cr, acknowledgment)

    @WithSpan
    @KafkaListener(
        topics = [NARMESTELEDER_ESYFO_LEESAH_TOPIC],
        containerFactory = "narmesteLederEsyfoKafkaListenerContainerFactory",
        autoStartup = "\${narmeste-leder.esyfo-consumer-aktiv:false}",
    )
    fun listenEsyfo(
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            oppdateringAvNarmesteLederEsyfo.behandle(cr)
            acknowledgment.acknowledge()
        } catch (_: Exception) {
            throw KafkaErrorHandlerException(
                message = "ESYFO-projeksjon feilet; record blir forsøkt igjen",
                errorHandlerLoggingEnabled = false,
            )
        }
    }

    private fun behandle(
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) {
        val melding = cr.value()
        if (melding == null) {
            log.info("Ignorerer tombstone for nærmeste leder")
        } else {
            try {
                oppdateringAvNarmesteLeder.behandleMeldingFraKafka(melding)
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
        }
        acknowledgment.acknowledge()
    }
}

const val NARMESTELEDER_LEESAH_TOPIC = "teamsykmelding.syfo-narmesteleder-leesah"
const val NARMESTELEDER_ESYFO_LEESAH_TOPIC = "team-esyfo.syfo-narmesteleder-leesah"
