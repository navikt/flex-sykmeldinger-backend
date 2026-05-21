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
                .onStatus(HttpStatusCode::is4xxClientError) { _, httpResponse ->
                    when (httpResponse.statusCode.value()) {
                        401 -> log.warn("Ugyldig token ved kall til harSoknad for sykmelding $sykmeldingUuid")
                        403 -> log.warn("Søknaden tilhører annen bruker ved kall til harSoknad for sykmelding $sykmeldingUuid")
                        else -> throw RuntimeException(
                            "Uventet 4xx-feil (${httpResponse.statusCode.value()}) ved kall til harSoknad for sykmelding $sykmeldingUuid",
                        )
                    }
                }
                .toEntity<HarSoknadResponse>()

        return response.body?.harSoknad ?: false
    }
}

data class HarSoknadResponse(
    val harSoknad: Boolean,
)
