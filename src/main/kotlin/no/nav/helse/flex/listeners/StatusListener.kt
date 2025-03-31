package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.producers.sykmeldingstatus.STATUS_LEESAH_SOURCE
import no.nav.helse.flex.producers.sykmeldingstatus.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.sykmelding.application.StatusHandterer
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class StatusListener(
    private val statusHandterer: StatusHandterer,
) {
    val log = logger()

    @KafkaListener(
        topics = [SYKMELDINGSTATUS_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        // TODO: Hvordan offset?
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) = try {
        log.info("Mottatt status for sykmelding ${cr.key()}")
        val status: SykmeldingStatusKafkaMessageDTO = objectMapper.readValue(cr.value())
        if (status.kafkaMetadata.source != STATUS_LEESAH_SOURCE) {
            statusHandterer.handterStatus(status)
        } else {
            log.info("Hendelse er fra flex-sykmeldinger-backend, ignorerer")
        }
    } catch (e: Exception) {
        log.warn("Feil ved håndtering av status for sykmelding ${cr.key()}", e)
    } finally {
        acknowledgment.acknowledge()
    }
}
