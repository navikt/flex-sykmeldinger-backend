package no.nav.helse.flex.gateways

import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.tsmsykmeldingstatus.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingStatusHandterer
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.function.Supplier

@Component
class SykmeldingStatusKafkaListener(
    private val sykmeldingStatusHandterer: SykmeldingStatusHandterer,
    private val nowFactory: Supplier<Instant> = Supplier { Instant.now() },
    private val environmentToggles: EnvironmentToggles,
) {
    val log = logger()

    @WithSpan
    @KafkaListener(
        topics = [SYKMELDINGSTATUS_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
        id = "flex-sykmeldinger-backend-consumer-earliest",
        concurrency = "4",
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            prosesserKafkaRecord(cr)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            throw KafkaErrorHandlerException(
                cause = e,
                insecureMessage = "Feil ved prosessering av sykmelding status på kafka",
            )
        }
    }

    internal fun prosesserKafkaRecord(cr: ConsumerRecord<String, String>) {
        log.info("Mottatt status for sykmelding '${cr.key()}' på kafka topic '${cr.topic()}'")
        val verdi = cr.value()
        if (verdi == null || verdi == "null") {
            log.info("Mottatt tombstone på sykmeldingstatus, vi ignorerer dette: ${cr.key()}")
            return
        }

        val status: SykmeldingStatusKafkaMessageDTO =
            try {
                objectMapper.readValue(verdi)
            } catch (e: Exception) {
                throw KafkaErrorHandlerException(
                    cause = e,
                    insecureMessage = "Feil ved deserialisering",
                )
            }

        if (environmentToggles.isDevelopment() && status.erEldreEnn(dager = 60)) {
            log.warn("Sykmelding status er eldre enn 60 dager, ignorerer: ${status.kafkaMetadata.sykmeldingId}")
            return
        }

        try {
            sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        } catch (e: Exception) {
            throw KafkaErrorHandlerException(
                cause = e,
                insecureMessage =
                    "Feil ved håndtering, " +
                        mapOf("sykmeldingId" to status.kafkaMetadata.sykmeldingId, "status" to status.event.statusEvent),
            )
        }
    }

    private fun SykmeldingStatusKafkaMessageDTO.erEldreEnn(dager: Long): Boolean {
        val nåTid = nowFactory.get()
        val tidsGrense = nåTid.minus(dager, ChronoUnit.DAYS)
        val statusTid = this.event.timestamp
        return statusTid.toInstant().isBefore(tidsGrense)
    }
}
