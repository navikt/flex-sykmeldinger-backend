package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.sykmelding.application.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.sykmelding.application.SykmeldingStatusHandterer
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
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        acknowledgment.acknowledge()
    } catch (e: Exception) {
        log.warn("Feil ved håndtering av status for sykmelding ${cr.key()}", e)
    }
}
