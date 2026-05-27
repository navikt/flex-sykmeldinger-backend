package no.nav.helse.flex.gateways.sykepengesoknadbackend

import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Component
class SykepengesoknadBackendEksternClient(
    private val sykepengesoknadBackendRestClient: RestClient,
) : SykepengesoknadBackendClient {
    @Retryable
    override fun harSoknad(sykmeldingUuid: String): Boolean {
        val response =
            sykepengesoknadBackendRestClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/api/v2/soknader/sykmelding/$sykmeldingUuid/harSoknad").build()
                }.retrieve()
                .toEntity<HarSoknadResponse>()

        return response.body?.harSoknad
            ?: throw RuntimeException("harSoknad response inneholdt ikke data for sykmelding $sykmeldingUuid")
    }

    @Retryable
    override fun opprettOptIn(sykmeldingKafkaMessage: SykmeldingKafkaMessage) {
        sykepengesoknadBackendRestClient
            .post()
            .uri("/api/v2/soknader/opprett-opt-in")
            .body(sykmeldingKafkaMessage)
            .retrieve()
            .toEntity<Unit>()
    }
}

data class HarSoknadResponse(
    val harSoknad: Boolean,
)
