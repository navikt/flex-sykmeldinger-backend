package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.sykmelding.domain.SykmeldingKafkaRecord
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class TestRunSykmeldingListener {
    val log = logger()

    @KafkaListener(
        topics = [SYKMELDING_TOPIC],
        containerFactory = "aivenKafkaBatchListenerContainerFactory",
        properties = [
            "auto.offset.reset = earliest",
            "${ConsumerConfig.MAX_POLL_RECORDS_CONFIG} = 1000",
        ],
        id = "flex-sykmeldinger-backend-test-1",
    )
    fun listen(
        consumerRecords: ConsumerRecords<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        for (record in consumerRecords) {
            try {
                val key: String = record.key()
                val value: SykmeldingKafkaRecord? =
                    record.value()?.let { objectMapper.readValue<SykmeldingKafkaRecord>(it) }
                if (value == null) {
                    continue
                }
                val sykmeldingId = value.sykmelding.id

                val manglendeSykmeldingId = "280c5631-1217-4bae-8ee8-f875a10718b3"

                if (sykmeldingId == manglendeSykmeldingId) {
                    log.info("Test sykmelding listener fant sykmelding med sykmeldingId: $sykmeldingId. Mottatt på topic $SYKMELDING_TOPIC")
                }

                if (sykmeldingId != key) {
                    throw IllegalArgumentException(
                        "SykmeldingId i key og sykmeldingId i value er ikke like. Key: $key, value: $sykmeldingId",
                    )
                }
            } catch (e: Exception) {
                throw RuntimeException("Feil ved behandling av sykmelding på kafka. Melding key: ${record.key()}")
            }
            acknowledgment.acknowledge()
        }
    }
}
