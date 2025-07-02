package no.nav.helse.flex.listeners

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.config.EnvironmentToggles
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
    private val environmentToggles: EnvironmentToggles,
) {
    val log = logger()

    @KafkaListener(
        topics = [SYKMELDING_TOPIC],
        containerFactory = "aivenKafkaListenerContainerFactory",
        properties = ["auto.offset.reset = earliest"],
        id = "flex-sykmeldinger-backend-2",
        concurrency = "4",
    )
    fun listen(
        cr: ConsumerRecord<String, String>,
        acknowledgment: Acknowledgment,
    ) {
        try {
            prosesserKafkaRecord(cr)
            acknowledgment.acknowledge()
        } catch (_: Exception) {
            throw RuntimeException("Feil ved behandling av sykmelding på kafka. Melding key: ${cr.key()}")
        }
    }

    internal fun prosesserKafkaRecord(cr: ConsumerRecord<String, String>) {
        val sykmeldingId = cr.key()
        val serialisertSykmelding: String? = cr.value()

        if (burdeIgnorereSykmelding(serialisertSykmelding, sykmeldingId = sykmeldingId)) {
            return
        }

        val sykmeldingRecord: SykmeldingKafkaRecord? =
            if (serialisertSykmelding == null) {
                null
            } else {
                try {
                    objectMapper.readValue(serialisertSykmelding)
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

    private fun burdeIgnorereSykmelding(
        sykmeldingRecordJson: String?,
        sykmeldingId: String? = null,
    ): Boolean {
        if (environmentToggles.isProduction()) {
            return false
        }
        if (sykmeldingRecordJson == null) {
            return false
        }

        val minimalSykmeldingKafkaRecord: MinimalSykmeldingKafkaRecord =
            try {
                objectMapper.readValue(sykmeldingRecordJson)
            } catch (e: Exception) {
                log.error(
                    "Feil minimal sykmelding format. sykmeldingId: $sykmeldingId. Rå sykmelding: $sykmeldingRecordJson",
                    e,
                )
                throw e
            }
        if (minimalSykmeldingKafkaRecord.sykmelding.type == "DIGITAL") {
            log.info(
                "Ignorerer sykmelding av type DIGITAL, sykmeldingId: ${minimalSykmeldingKafkaRecord.sykmelding.id}",
            )
            return true
        } else {
            return false
        }
    }

    private data class MinimalSykmeldingKafkaRecord(
        val sykmelding: MinimalSykmelding,
    )

    private data class MinimalSykmelding(
        val id: String,
        val type: String,
        val metadata: Map<String, Any?>? = null,
    )
}

const val SYKMELDING_TOPIC = "tsm.sykmeldinger"
