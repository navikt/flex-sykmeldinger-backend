package no.nav.helse.flex.clients.syketilfelle

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Component
class SyketilfelleEksternClient(
    private val syketilfelleRestClient: RestClient,
) : SyketilfelleClient {
    val log = logger()

    @Retryable
    override fun getErUtenforVentetid(
        identer: PersonIdenter,
        sykmeldingId: String,
    ): Boolean {
        val uri =
            syketilfelleRestClient.post().uri { uriBuilder ->
                uriBuilder.path("/api/v1/ventetid/$sykmeldingId/erUtenforVentetid").build()
            }
        val res =
            uri
                .body(
                    VentetidRequest(
                        fnr = identer.alle().joinToString(", "),
                        sykmeldingId = sykmeldingId,
                    ).serialisertTilString(),
                ).retrieve()
                .toEntity<Boolean>()
                .body

        return res ?: throw RuntimeException("Klarte ikke hente ventetid for sykmelding $sykmeldingId")
    }
}

data class VentetidRequest(
    val fnr: String,
    val sykmeldingId: String,
    val hentAndreIdenter: Boolean = true,
)
