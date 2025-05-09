package no.nav.helse.flex.listeners

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.producers.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.tsmsykmeldingstatus.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingStatusHandterer
import no.nav.helse.flex.utils.LogMarker
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class SykmeldingStatusListener(
    private val sykmeldingStatusHandterer: SykmeldingStatusHandterer,
    private val environmentToggles: EnvironmentToggles,
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
    ) {
        if (environmentToggles.isProduction()) {
            log.info("SykmeldingStatus listener er skrudd av i prod. Hopper over melding med key: ${cr.key()}")
            return
        }
        try {
            log.info("Mottatt status for sykmelding ${cr.key()}")
            val status: SykmeldingStatusKafkaMessageDTO = objectMapper.readValue(cr.value())
            sykmeldingStatusHandterer.lagreSykmeldingStatus(status)
            acknowledgment.acknowledge()
        } catch (e: JacksonException) {
            log.error("Feil sykmelding status format. Melding key: ${cr.key()}. Se secure logs")
            log.error(LogMarker.SECURE_LOGS, "Feil sykmelding status format. Melding key: ${cr.key()}", e)
            throw e
        } catch (e: Exception) {
            log.error("Exception ved sykmelding status håndtering. Melding key: ${cr.key()}. Se secure logs")
            log.error(LogMarker.SECURE_LOGS, "Exception ved sykmelding status håndtering. Melding key: ${cr.key()}", e)
            throw e
        }
    }
}
