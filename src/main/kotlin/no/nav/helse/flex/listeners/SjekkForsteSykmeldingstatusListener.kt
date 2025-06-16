package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.producers.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.tsmsykmeldingstatus.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class SjekkForsteSykmeldingstatusListener {
    private val log = logger()
    private var harLestForste: Boolean = false

    @KafkaListener(
        topics = [SYKMELDINGSTATUS_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
        id = "flex-sykmeldinger-backend-test-sjekk-forste-status",
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        if (harLestForste) {
            return
        }

        try {
            prosesserKafkaRecord(cr)
        } catch (e: Exception) {
            throw RuntimeException("Feil ved behandling av sykmelding status på kafka, meldingKey: ${cr.key()}")
        }

        harLestForste = true
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

        log.info("Første sykmeldingstatus på kafka, kafkaMetadata: ${status.kafkaMetadata.timestamp}, event: ${status.event.timestamp}")
    }
}
