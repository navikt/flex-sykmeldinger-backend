package no.nav.helse.flex.gateways

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.stereotype.Component
import java.time.LocalDateTime

const val SYKMELDING_BRUKERNOTIFIKASJON_TOPIC = "teamsykmelding.sykmeldingnotifikasjon"

interface SykmeldingBrukernotifikasjonProducer {
    fun produserSykmeldingBrukernotifikasjon(sykmeldingNotifikasjon: SykmeldingNotifikasjon)
}

@Component
class SykmeldingBrukernotifikasjonKafkaProducer(
    private val meldingProducer: Producer<String, String>,
) : SykmeldingBrukernotifikasjonProducer {
    private val log = logger()

    @WithSpan
    override fun produserSykmeldingBrukernotifikasjon(sykmeldingNotifikasjon: SykmeldingNotifikasjon) {
        try {
            meldingProducer.send(
                ProducerRecord(
                    SYKMELDING_BRUKERNOTIFIKASJON_TOPIC,
                    sykmeldingNotifikasjon.sykmeldingId,
                    sykmeldingNotifikasjon.serialisertTilString(),
                ),
            )
        } catch (ex: Exception) {
            log.error("Kunne ikke produsere brukernotifikasjon for sykmelding med id ${sykmeldingNotifikasjon.sykmeldingId}", ex)
            throw ex
        }
    }
}

data class SykmeldingNotifikasjon(
    val sykmeldingId: String,
    val status: SykmeldingNotifikasjonStatus,
    val mottattDato: LocalDateTime,
    val fnr: String,
)

enum class SykmeldingNotifikasjonStatus {
    OK,
    MANUAL_PROCESSING,
    INVALID,
}
