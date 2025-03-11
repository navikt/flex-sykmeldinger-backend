package no.nav.helse.flex.clients.aareg

import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.springframework.http.HttpStatus
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Component
class AaregEksternClient(
    private val aaregRestClient: RestClient,
    private val environmentToggles: EnvironmentToggles,
) : AaregClient {
    val log = logger()

    @Retryable(
        value = [HttpServerErrorException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 2000, multiplier = 2.0),
    )
    override fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse {
        try {
            val uri =
                aaregRestClient.post().uri { uriBuilder ->
                    uriBuilder
                        .path("/api/v2/arbeidstaker/arbeidsforholdoversikt")
                        .build()
                }
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
        } catch (e: HttpServerErrorException) {
            when (e.statusCode) {
                HttpStatus.INTERNAL_SERVER_ERROR -> {
                    if (environmentToggles.isProduction()) {
                        throw e
                    } else {
                        log.warn(
                            "AAREG er midlertidig nede i dev. Returnerer tom liste etter retry.",
                            e,
                        )
                        return ArbeidsforholdoversiktResponse(emptyList())
                    }
                }
                else -> {
                    throw e
                }
            }
        }
    }
}
