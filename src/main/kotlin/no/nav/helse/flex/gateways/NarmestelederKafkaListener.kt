package no.nav.helse.flex.gateways

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.narmesteleder.NarmesteLederEsyfoCheckpoint
import no.nav.helse.flex.narmesteleder.OppdateringAvNarmesteLeder
import no.nav.helse.flex.narmesteleder.OppdateringAvNarmesteLederEsyfo
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class NarmestelederListener(
    private val oppdateringAvNarmesteLeder: OppdateringAvNarmesteLeder,
    private val oppdateringAvNarmesteLederEsyfo: OppdateringAvNarmesteLederEsyfo,
    private val esyfoCheckpoint: NarmesteLederEsyfoCheckpoint,
    private val meterRegistry: MeterRegistry,
) {
    private val log = logger()
    private val checkpointMalere = ConcurrentHashMap<Int, Gauge>()

    @EventListener(ApplicationReadyEvent::class)
    fun registrerLagredeCheckpointMalere() {
        esyfoCheckpoint.hentPartisjoner(NARMESTELEDER_ESYFO_LEESAH_TOPIC).forEach(::registrerCheckpointMaler)
    }

    private fun registrerCheckpointMaler(partisjon: Int) {
        checkpointMalere.computeIfAbsent(partisjon) {
            Gauge
                .builder("narmeste_leder_esyfo_db_checkpoint_offset", esyfoCheckpoint) { checkpoint ->
                    runCatching { checkpoint.hentOffset(NARMESTELEDER_ESYFO_LEESAH_TOPIC, partisjon)?.toDouble() ?: Double.NaN }
                        .getOrDefault(Double.NaN)
                }.tag("topic", NARMESTELEDER_ESYFO_LEESAH_TOPIC)
                .tag("partition", partisjon.toString())
                .register(meterRegistry)
        }
    }

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
            val pafort = oppdateringAvNarmesteLederEsyfo.behandle(cr)
            registrerCheckpointMaler(cr.partition())
            meterRegistry
                .counter(
                    "narmeste_leder_esyfo_records",
                    "topic",
                    NARMESTELEDER_ESYFO_LEESAH_TOPIC,
                    "partition",
                    cr.partition().toString(),
                    "resultat",
                    if (pafort) "pafort" else "duplikat",
                    "type",
                    if (cr.value() == null) "tombstone" else "verdi",
                ).increment()
            acknowledgment.acknowledge()
        } catch (_: Exception) {
            meterRegistry
                .counter(
                    "narmeste_leder_esyfo_records",
                    "topic",
                    NARMESTELEDER_ESYFO_LEESAH_TOPIC,
                    "partition",
                    cr.partition().toString(),
                    "resultat",
                    "feil",
                    "type",
                    if (cr.value() == null) "tombstone" else "verdi",
                ).increment()
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
