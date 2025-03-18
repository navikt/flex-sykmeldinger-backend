package no.nav.helse.flex.listeners

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.sykmelding.application.SykmeldingKafkaLagrer
import no.nav.helse.flex.sykmelding.domain.SykmeldingKafkaRecord
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class SykmeldingListener(
    private val sykmeldingKafkaLagrer: SykmeldingKafkaLagrer,
    private val environmentToggles: EnvironmentToggles,
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
        if (environmentToggles.isProduction()) {
            return
        }
        try {
            val sykmeldingMedBehandlingsutfall: SykmeldingKafkaRecord =
                objectMapper.readValue(cr.value())
            sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)
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

const val SYKMELDING_TOPIC = "tsm.tsm-sykmeldinger"
