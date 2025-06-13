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
    override fun getArbeidstakerArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse {
        val res =
            aaregRestClient
                .post()
                .uri { uriBuilder -> uriBuilder.path("/api/v2/arbeidstaker/arbeidsforholdoversikt").build() }
                .headers {
                    it.contentType = MediaType.APPLICATION_JSON
                }.body(
                    FinnArbeidsforholdoversikterPrArbeidstakerAPIRequest(
                        arbeidstakerId = fnr,
                        arbeidsforholdtyper = listOf("ordinaertArbeidsforhold"),
                        arbeidsforholdstatuser = listOf("AKTIV", "AVSLUTTET"),
                    ).serialisertTilString(),
                ).retrieve()
                .toEntity<ArbeidsforholdoversiktResponse>()
                .body

        return res ?: throw RuntimeException("getArbeidstakerArbeidsforholdoversikt response inneholdt ikke data")
    }

    override fun getArbeidsstedArbeidsforholdoversikt(arbeidsstedOrgnummer: String): ArbeidsforholdoversiktResponse {
        val res =
            aaregRestClient
                .post()
                .uri { uriBuilder -> uriBuilder.path("/api/v2/arbeidssted/arbeidsforholdoversikt").build() }
                .headers {
                    it.contentType = MediaType.APPLICATION_JSON
                }.body(
                    FinnArbeidsforholdoversikterPrArbeidsstedAPIRequest(
                        arbeidsstedId = arbeidsstedOrgnummer,
                        arbeidsforholdtyper = listOf("ordinaertArbeidsforhold"),
                        arbeidsforholdstatuser = listOf("AKTIV", "AVSLUTTET"),
                    ).serialisertTilString(),
                ).retrieve()
                .toEntity<ArbeidsforholdoversiktResponse>()
                .body

        return res ?: throw RuntimeException("getArbeidsstedArbeidsforholdoversikt response inneholdt ikke data")
    }
}
