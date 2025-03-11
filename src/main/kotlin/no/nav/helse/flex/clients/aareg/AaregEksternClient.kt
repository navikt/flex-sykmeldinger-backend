package no.nav.helse.flex.clients.aareg

import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.springframework.http.*
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Component
class AaregEksternClient(
    private val aaregRestClient: RestClient,
) : AaregClient {
    val log = logger()

    @Retryable
    override fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse {
        val uri = aaregRestClient.post().uri { uriBuilder -> uriBuilder.path("/api/v2/arbeidstaker/arbeidsforholdoversikt").build() }
        val res =
            uri
                .body(
                    ArbeidsforholdRequest(
                        arbeidstakerId = fnr,
                        arbeidsforholdtyper = listOf("ordinaertArbeidsforhold"),
                        arbeidsforholdstatuser = listOf("AKTIV", "AVSLUTTET"),
                    ).serialisertTilString(),
                ).retrieve()
                .toEntity<ArbeidsforholdoversiktResponse>()
                .body

        return res ?: throw RuntimeException("getArbeidsforholdoversikt response inneholdt ikke data")
    }
}
