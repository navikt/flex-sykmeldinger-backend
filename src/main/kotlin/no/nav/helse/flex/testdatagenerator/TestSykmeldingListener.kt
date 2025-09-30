package no.nav.helse.flex.testdatagenerator

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.sykmelding.SykmeldingKafkaLagrer
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
        val sykmeldingId = cr.key()
        val serialisertHendelse = cr.value()
        try {
            val sykmeldingRecord: SykmeldingKafkaRecord? =
                if (serialisertHendelse == null) {
                    null
                } else {
                    objectMapper.readValue(serialisertHendelse)
                }
            sykmeldingKafkaLagrer.lagreSykmeldingFraKafka(
                sykmeldingId = sykmeldingId,
                sykmeldingKafkaRecord = sykmeldingRecord,
            )
            log.info(
                "Motatt sykmelding fra $TEST_SYKMELDING_TOPIC: \n${
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                        sykmeldingRecord,
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
