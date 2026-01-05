package no.nav.helse.flex.gateways.syketilfelle

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.utils.logger
import org.springframework.retry.annotation.Retryable
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
    override fun getErUtenforVentetid(
        identer: PersonIdenter,
        sykmeldingId: String,
    ): ErUtenforVentetidResponse {
        val uri =
            syketilfelleRestClient.get().uri { uriBuilder ->
                uriBuilder.path("/api/bruker/v2/ventetid/$sykmeldingId/erUtenforVentetid").build()
            }
        val res = uri.retrieve().toEntity<ErUtenforVentetidResponse>().body

        return res ?: throw RuntimeException("Klarte ikke hente ventetid for sykmelding $sykmeldingId")
    }
}

data class ErUtenforVentetidResponse(
    val erUtenforVentetid: Boolean,
    val oppfolgingsdato: LocalDate?,
    val ventetid: FomTomPeriode? = null,
)

data class FomTomPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
)
