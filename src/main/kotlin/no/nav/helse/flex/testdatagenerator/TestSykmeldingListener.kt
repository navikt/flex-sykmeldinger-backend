package no.nav.helse.flex.testdatagenerator

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.sykmelding.application.SykmeldingKafkaLagrer
import no.nav.helse.flex.sykmelding.domain.SykmeldingKafkaRecord
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("testdata")
class TestSykmeldingListener(
    private val sykmeldingKafkaLagrer: SykmeldingKafkaLagrer,
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
            val sykmeldingMedBehandlingsutfall: SykmeldingKafkaRecord =
                objectMapper.readValue(cr.value())
            sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)
            log.info(
                "Motatt sykmelding med behandlingsutfall: \n${
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        sykmeldingMedBehandlingsutfall,
                    )
                }",
            )
        } catch (e: JacksonException) {
            log.error("Feil sykmelding format. Denne blir skippet. Melding key: ${cr.key()}. Value: ${cr.value()}", e)
        } catch (e: Exception) {
            log.error("Exception ved håndtering av sykmelding", e)
        } finally {
            acknowledgment.acknowledge()
        }
    }
}

const val TEST_SYKMELDING_TOPIC = "flex.test-sykmelding"
