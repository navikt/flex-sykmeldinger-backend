package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
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

@Component
class SykmeldingStatusListener(
    private val sykmeldingStatusHandterer: SykmeldingStatusHandterer,
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
        log.info("Mottatt status for sykmelding: ${cr.key()}")
        val status: SykmeldingStatusKafkaMessageDTO =
            try {
                objectMapper.readValue(cr.value())
            } catch (e: Exception) {
                log.errorSecure(
                    "Feil sykmelding status format, meldingKey: ${cr.key()}",
                    secureMessage = "Rå sykmelding status: ${cr.value()}",
                    secureThrowable = e,
                )
                throw e
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
