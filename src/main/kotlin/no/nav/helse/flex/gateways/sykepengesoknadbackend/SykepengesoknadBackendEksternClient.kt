package no.nav.helse.flex.gateways.sykepengesoknadbackend

import no.nav.helse.flex.utils.logger
import org.springframework.http.HttpStatusCode
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Component
class SykepengesoknadBackendEksternClient(
    private val sykepengesoknadBackendRestClient: RestClient,
) : SykepengesoknadBackendClient {
    val log = logger()

    @Retryable
    override fun harSoknad(sykmeldingUuid: String): Boolean {
        val response =
            sykepengesoknadBackendRestClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/api/v2/soknader/sykmelding/$sykmeldingUuid/harSoknad").build()
                }
                .retrieve()
                .onStatus(HttpStatusCode::isError) { _, httpResponse ->
                    throw RuntimeException(
                        "Kall til harSoknad feilet med HTTP-${httpResponse.statusCode.value()} for sykmelding $sykmeldingUuid",
                    ).also {
                        log.error(it.message)
                    }
                }
                .toEntity<HarSoknadResponse>()

        return response.body?.harSoknad
            ?: throw RuntimeException("harSoknad response inneholdt ikke data for sykmelding $sykmeldingUuid")
    }
}

data class HarSoknadResponse(
    val harSoknad: Boolean,
)
