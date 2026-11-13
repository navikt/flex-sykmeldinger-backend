package no.nav.helse.flex.gateways

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.narmesteleder.OppdateringAvNarmesteLeder
import no.nav.helse.flex.utils.KafkaKonsumerUtils
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class NarmestelederEsyfoListener(
    private val oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder,
) {
    private val log = logger()

    @WithSpan
    @KafkaListener(
        topics = [NARMESTELEDER_ESYFO_LEESAH_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = latest"],
        containerPostProcessor = NARMESTELEDER_STARTPOSISJON,
        autoStartup = "\${narmeste-leder.esyfo-listener-aktiv:false}",
    )
    fun listen(
        cr: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) = KafkaKonsumerUtils.medMdcMetadata(cr) {
        try {
            val verdi = cr.value()
            if (verdi == null) {
                oppdateringAvNarmesteLeder.behandleTombstone(UUID.fromString(cr.key()))
            } else {
                oppdateringAvNarmesteLeder.behandleMeldingFraKafka(verdi)
            }
        } catch (e: Exception) {
            log.errorSecure(
                message = "Feil ved håndtering av nærmeste leder hendelse fra eSyfo, exception: ${e::class.simpleName}. Blir retryet",
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

const val NARMESTELEDER_ESYFO_LEESAH_TOPIC = "team-esyfo.syfo-narmesteleder-leesah"
