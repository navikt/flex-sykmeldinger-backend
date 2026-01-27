package no.nav.helse.flex.gateways

import com.fasterxml.jackson.module.kotlin.readValue
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.config.kafka.KafkaErrorHandlerException
import no.nav.helse.flex.sykmelding.EksternSykmeldingHandterer
import no.nav.helse.flex.sykmelding.EksternSykmeldingMelding
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class SykmeldingListener(
    private val eksternSykmeldingHandterer: EksternSykmeldingHandterer,
    private val environmentToggles: EnvironmentToggles,
) {
    val log = logger()

    @WithSpan
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
        } catch (e: Exception) {
            if (environmentToggles.isDevelopment()) {
                log.error("Ignorerer feil i dev", e)
                acknowledgment.acknowledge()
                return
            }
            log.errorSecure(
                message = "Feil ved prosessering av sykmelding på kafka, exception: ${e::class.simpleName}. Dette vil bli retryet",
                secureThrowable = e,
            )
            throw KafkaErrorHandlerException(
                message = "Feil ved prosessering av sykmelding på kafka",
                cause = e,
            )
        }
    }

    internal fun prosesserKafkaRecord(cr: ConsumerRecord<String, String>) {
        val sykmeldingId = cr.key()
        val serialisertSykmelding: String? = cr.value()

        val sykmeldingRecord: EksternSykmeldingMelding? =
            if (serialisertSykmelding == null) {
                null
            } else {
                try {
                    objectMapper.readValue(serialisertSykmelding)
                } catch (e: Exception) {
                    throw KafkaErrorHandlerException(
                        message = "Feil ved deserialisering",
                        cause = e,
                    )
                }
            }

        if (sykmeldingRecord != null) {
            if (sykmeldingId != sykmeldingRecord.sykmelding.id) {
                throw KafkaErrorHandlerException(
                    message =
                        "SykmeldingId i key og sykmeldingId i value er ikke like. Key: $sykmeldingId, " +
                            "value: ${sykmeldingRecord.sykmelding.id}",
                )
            }
        }

        log.info("Prosesserer sykmelding $sykmeldingId fra topic $SYKMELDING_TOPIC")

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = sykmeldingId,
            eksternSykmeldingMelding = sykmeldingRecord,
        )
    }
}

const val SYKMELDING_TOPIC = "tsm.sykmeldinger"
