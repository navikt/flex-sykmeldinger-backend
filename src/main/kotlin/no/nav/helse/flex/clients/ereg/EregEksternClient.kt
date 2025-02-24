package no.nav.helse.flex.clients.ereg

import no.nav.helse.flex.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Component
class EregEksternClient(
    private val plainRestTemplate: RestTemplate,
    @Value("\${EREG_URL}") private val eregUrl: String,
) : EregClient {
    val log = logger()

    @Retryable
    override fun hentNokkelinfo(orgnummer: String): Nokkelinfo {
        val uriBuilder =
            UriComponentsBuilder.fromUriString("$eregUrl/v2/organisasjon/$orgnummer/noekkelinfo")

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val result =
            try {
                plainRestTemplate
                    .exchange(
                        uriBuilder.toUriString(),
                        HttpMethod.GET,
                        HttpEntity<Any>(headers),
                        Nokkelinfo::class.java,
                    )
            } catch (e: Exception) {
                log.error("hentNokkelinfo kall mot ereg feilet $e")
                throw e
            }
        return result.body
            ?: throw RuntimeException("getArbeidsforholdoversikt response inneholdt ikke data")
    }
}
