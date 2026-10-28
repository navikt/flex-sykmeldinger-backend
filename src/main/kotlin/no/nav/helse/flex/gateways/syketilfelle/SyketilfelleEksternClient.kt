package no.nav.helse.flex.gateways.syketilfelle

import no.nav.helse.flex.utils.logger
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.LocalDate

@Component
class SyketilfelleEksternClient(
    private val syketilfelleRestClient: RestClient,
) : SyketilfelleClient {
    val log = logger()

    @Retryable
    override fun getVentetidForSykmelding(sykmeldingId: String): VentetidForSykmeldingResponse {
        val uri =
            syketilfelleRestClient.get().uri { uriBuilder ->
                uriBuilder.path("/api/bruker/v2/ventetid/$sykmeldingId/ventetidForSykmelding").build()
            }
        val res = uri.retrieve().toEntity<VentetidForSykmeldingResponse>().body

        return res ?: throw RuntimeException("Klarte ikke hente ventetidForSykmelding for sykmelding $sykmeldingId")
    }
}

data class FomTomPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class SammeVentetidPeriode(
    val ressursId: String,
    val ventetid: FomTomPeriode,
)

data class VentetidForSykmeldingResponse(
    val erUtenforVentetid: Boolean,
    val periodeMedSammeVentetid: List<SammeVentetidPeriode>,
)
