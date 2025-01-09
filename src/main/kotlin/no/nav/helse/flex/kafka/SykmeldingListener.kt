package no.nav.helse.flex.kafka

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfall
import no.nav.helse.flex.sykmelding.logikk.SykmeldingLagrer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("testdata")
class SykmeldingListener(
    private val sykmeldingLagrer: SykmeldingLagrer,
) {
    val log = logger()

    @KafkaListener(
        topics = [SYKMELDING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = latest"],
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        val sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfall =
            try {
                objectMapper.readValue(cr.value())
            } catch (e: Exception) {
                log.error("Feil sykmelding data: ${cr.value()}")
                log.error("Exception ved feil sykmelding konvertering", e)
                // TODO: fjern og kast feil
                acknowledgment.acknowledge()
                return
            }
        log.info(
            "Motatt sykmelding med behandlingsutfall: \n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                sykmeldingMedBehandlingsutfall,
            )}",
        )

        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)
        acknowledgment.acknowledge()
    }
}

// TODO: endre når tsm har klart topic
const val SYKMELDING_TOPIC = "flex.test-sykmelding"
