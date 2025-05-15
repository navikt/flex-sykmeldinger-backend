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
            if (cr.value() == null) {
                log.warn("Mottok sykmelding tombstone, key: ${cr.key()}. Ikke implementert, hopper over denne uten ack")
                return
            }
            prosesserKafkaRecord(cr)
            acknowledgment.acknowledge()
        } catch (e: Exception) {
            throw RuntimeException("Feil ved behandling av sykmelding på kafka. Melding key: ${cr.key()}")
        }
    }

    internal fun prosesserKafkaRecord(cr: ConsumerRecord<String, String>) {
        val value = cr.value()
        val sykmeldingRecord: SykmeldingKafkaRecord =
            try {
                objectMapper.readValue(value)
            } catch (e: Exception) {
                log.errorSecure(
                    "Feil sykmelding format. Melding key: ${cr.key()}",
                    secureMessage = "Rå sykmelding: ${cr.value()}",
                    secureThrowable = e,
                )
                throw e
            }
        log.info("Mottok sykmelding med id ${sykmeldingRecord.sykmelding.id} fra topic $SYKMELDING_TOPIC")
        try {
            sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingRecord)
        } catch (e: Exception) {
            log.errorSecure(
                "Feil ved sykmelding håndtering. sykmeldingId: ${sykmeldingRecord.sykmelding.id}, meldingKey: ${cr.key()}",
                secureThrowable = e,
            )
            throw e
        }
    }
}

const val SYKMELDING_TOPIC = "tsm.sykmeldinger"
