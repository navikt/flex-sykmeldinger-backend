package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.sykmelding.application.SykmeldingKafkaLagrer
import no.nav.helse.flex.sykmelding.domain.SykmeldingKafkaRecord
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class SykmeldingListener(
    private val sykmeldingKafkaLagrer: SykmeldingKafkaLagrer,
) {
    val log = logger()

    @KafkaListener(
        topics = [SYKMELDING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        // TODO: Hvordan offset?
        properties = ["auto.offset.reset = latest"],
        groupId = "flex-sykmeldinger-backend-1",
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            prosesserKafkaRecord(cr)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            throw RuntimeException("Feil ved behandling av sykmelding på kafka. Melding key: ${cr.key()}")
        }
    }

    internal fun prosesserKafkaRecord(cr: ConsumerRecord<String, String>) {
        val sykmeldingId = cr.key()
        val serialisertHendelse = cr.value()
        val sykmeldingRecord: SykmeldingKafkaRecord? =
            if (serialisertHendelse == null) {
                null
            } else {
                try {
                    objectMapper.readValue(serialisertHendelse)
                } catch (e: Exception) {
                    log.errorSecure(
                        "Feil sykmelding format. Melding key: ${cr.key()}",
                        secureMessage = "Rå sykmelding: ${cr.value()}",
                        secureThrowable = e,
                    )
                    throw e
                }
            }

        if (sykmeldingRecord != null) {
            if (sykmeldingId != sykmeldingRecord.sykmelding.id) {
                val message =
                    "SykmeldingId i key og sykmeldingId i value er ikke like. Key: $sykmeldingId, " +
                        "value: ${sykmeldingRecord.sykmelding.id}"
                log.error(message)
                throw IllegalArgumentException(message)
            }
        }

        log.info("Prosesserer sykmelding $sykmeldingId fra topic $SYKMELDING_TOPIC")
        try {
            sykmeldingKafkaLagrer.lagreSykmeldingFraKafka(
                sykmeldingId = sykmeldingId,
                sykmeldingKafkaRecord = sykmeldingRecord,
            )
        } catch (e: Exception) {
            log.errorSecure(
                "Feil ved håndtering av sykmelding $sykmeldingId",
                secureThrowable = e,
            )
            throw e
        }
    }
}

const val SYKMELDING_TOPIC = "tsm.sykmeldinger"
