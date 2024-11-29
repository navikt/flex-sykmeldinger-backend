package no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient

import no.nav.helse.flex.logger
import no.nav.helse.flex.serialisertTilString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.web.client.RestTemplate

// @Component
class AaregClient(
    @Value("\${AAREG_URL}")
    private val url: String,
    private val aaregRestTemplate: RestTemplate,
) {
    val log = logger()

    private val arbeidsforholdPath = "$url/api/v2/arbeidstaker/arbeidsforhold"

    fun hentArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val result =
            aaregRestTemplate
                .exchange(
                    "$url/api/v2/arbeidstaker/arbeidsforholdoversikt",
                    HttpMethod.POST,
                    HttpEntity(
                        ArbeidsforholdRequest(
                            arbeidstakerId = fnr,
                            arbeidsforholdtyper = listOf("ordinaertArbeidsforhold"),
                            arbeidsforholdstatuser = listOf("AKTIV", "AVSLUTTET"),
                        ).serialisertTilString(),
                        headers,
                    ),
                    ArbeidsforholdoversiktResponse::class.java,
                )

        if (result.statusCode != HttpStatus.OK) {
            val message = "Kall mot aareg feiler med HTTP-" + result.statusCode
            log.error(message)
            throw RuntimeException(message)
        }

        result.body?.let { return it }

        val message = "Kall mot aareg returnerer ikke data"
        log.error(message)
        throw RuntimeException(message)
    }
}
