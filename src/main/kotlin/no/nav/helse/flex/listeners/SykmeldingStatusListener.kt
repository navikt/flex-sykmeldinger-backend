package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.producers.SykmeldingStatusKafkaMessageDTO
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
import java.util.function.Supplier

@Component
class SykmeldingStatusListener(
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
        } catch (_: Exception) {
            throw RuntimeException("Feil ved behandling av sykmelding status på kafka, meldingKey: ${cr.key()}")
        }
    }

    internal fun prosesserKafkaRecord(cr: ConsumerRecord<String, String>) {
        log.info("Mottatt status for sykmelding '${cr.key()}' på kafka topic '${cr.topic()}'")
        val status: SykmeldingStatusKafkaMessageDTO =
            try {
                objectMapper.readValue(cr.value())
            } catch (e: Exception) {
                if (cr.value() == null) {
                    log.error("Mottatt tom sykmelding status, ignorerer: ${cr.key()}")
                    return
                }
                log.errorSecure(
                    "Feil sykmelding status format, meldingKey: ${cr.key()}",
                    secureMessage = "Rå sykmelding status: ${cr.value()}",
                    secureThrowable = e,
                )
                throw e
            }

        val toManederSiden = nowFactory.get().minus(60, java.time.temporal.ChronoUnit.DAYS)
        if (environmentToggles.isDevelopment() &&
            status.event.timestamp
                .toInstant()
                .isBefore(toManederSiden)
        ) {
            log.warn("Sykmelding status er eldre enn to måneder, ignorerer: ${status.kafkaMetadata.sykmeldingId}")
            return
        }

        try {
            sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        } catch (e: Exception) {
            log.errorSecure(
                "Feil ved håndtering av sykmelding status, sykmeldingId: ${status.kafkaMetadata.sykmeldingId}, status: ${status.event.statusEvent}, meldingKey: ${cr.key()}",
                "Rå sykmelding status: ${cr.value()}",
                secureThrowable = e,
            )
            throw e
        }
    }
}
