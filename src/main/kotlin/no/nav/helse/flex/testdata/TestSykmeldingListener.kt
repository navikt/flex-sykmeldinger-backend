package no.nav.helse.flex.testdata

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.logikk.SykmeldingLagrer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment

// @Component
@Profile("testdata")
class TestSykmeldingListener(
    private val sykmeldingLagrer: SykmeldingLagrer,
) {
    val log = logger()

    @KafkaListener(
        topics = [TEST_SYKMELDING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
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
            log.info(
                "Motatt sykmelding med behandlingsutfall: \n${
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        sykmeldingMedBehandlingsutfall,
                    )
                }",
            )
        } catch (e: Exception) {
            log.error("Feil sykmelding data, denne blir skippet: ${cr.value()}")
            log.error("Exception ved feil sykmelding konvertering", e)
        } finally {
            acknowledgment.acknowledge()
        }
    }
}

const val TEST_SYKMELDING_TOPIC = "flex.test-sykmelding"
