package no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient

import no.nav.helse.flex.logger
import no.nav.helse.flex.serialisertTilString
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class AaregClient(
    @Value("\${AAREG_URL}")
    private val url: String,
    private val restTemplate: RestTemplate,
) {
    val log = logger()

    @Retryable
    fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val result: ResponseEntity<ArbeidsforholdoversiktResponse> =
            try {
                restTemplate
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
            } catch (e: Exception) {
                log.error("getArbeidsforholdoversikt kall mot aareg feilet $e")
                throw e
            }
        return result.body
            ?: throw RuntimeException("getArbeidsforholdoversikt response inneholdt ikke data")
    }
}
