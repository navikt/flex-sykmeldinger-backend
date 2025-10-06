package no.nav.helse.flex.gateways.aareg

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
        val res =
            aaregRestClient
                .post()
                .uri { uriBuilder -> uriBuilder.path("/api/v2/arbeidstaker/arbeidsforholdoversikt").build() }
                .headers {
                    it.contentType = MediaType.APPLICATION_JSON
                }.body(
                    ArbeidsforholdRequest(
                        arbeidstakerId = fnr,
                        arbeidsforholdtyper = listOf("ordinaertArbeidsforhold", "maritimtArbeidsforhold", "forenkletOppgjoersordning"),
                        arbeidsforholdstatuser = listOf("AKTIV", "AVSLUTTET"),
                    ).serialisertTilString(),
                ).retrieve()
                .toEntity<ArbeidsforholdoversiktResponse>()
                .body

        return res ?: throw RuntimeException("getArbeidsforholdoversikt response inneholdt ikke data")
    }
}
