package no.nav.helse.flex.listeners

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.logikk.SykmeldingLagrer
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Profile("test") // TODO: Skru på når topic er prodsatt
@Component
class SykmeldingListener(
    private val sykmeldingLagrer: SykmeldingLagrer,
) {
    val log = logger()

    @KafkaListener(
        topics = [SYKMELDING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        // TODO: Hvordan offset?
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            val sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfallMelding =
                objectMapper.readValue(cr.value())
            sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)
            acknowledgment.acknowledge()
        } catch (e: JacksonException) {
            log.error("Feil sykmelding format. Melding key: ${cr.key()}")
            throw e
        } catch (e: Exception) {
            log.error("Exception ved sykmelding håndtering. Melding key: ${cr.key()}")
            throw e
        }
    }
}

// TODO: endre når tsm har klart topic
const val SYKMELDING_TOPIC = "flex.sykmelding"
