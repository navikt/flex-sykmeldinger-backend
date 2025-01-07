package no.nav.helse.flex.testdata

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfall
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Profile("testdata")
class TestSykmeldingListener() {
    val log = logger()

    var sisteSykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfall? = null

    @KafkaListener(
        topics = [TEST_SYKMELDING_TOPIC],
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
                throw e
            }
        this.sisteSykmeldingMedBehandlingsutfall = sykmeldingMedBehandlingsutfall
        log.info(
            "Motatt sykmelding med behandlingsutfall: \n${objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(
                sykmeldingMedBehandlingsutfall,
            )}",
        )
        acknowledgment.acknowledge()
    }
}

const val TEST_SYKMELDING_TOPIC = "flex.test-sykmelding"
