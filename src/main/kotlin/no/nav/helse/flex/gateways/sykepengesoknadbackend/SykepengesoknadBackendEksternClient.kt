package no.nav.helse.flex.gateways.sykepengesoknadbackend

import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage
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

    @Retryable(exclude = [SykepengesoknadBackendClientException::class])
    override fun harSoknad(sykmeldingUuid: String): Boolean {
        val response =
            sykepengesoknadBackendRestClient
                .get()
                .uri { uriBuilder ->
                    uriBuilder.path("/api/v2/soknader/sykmelding/$sykmeldingUuid/harSoknad").build()
                }.retrieve()
                .onStatus(HttpStatusCode::isError) { _, httpResponse ->
                    val exception =
                        when {
                            httpResponse.statusCode.is4xxClientError -> {
                                SykepengesoknadBackendClientException(
                                    "Kall til harSoknad feilet med HTTP-${httpResponse.statusCode.value()} for sykmelding $sykmeldingUuid",
                                )
                            }
                            else -> {
                                SykepengesoknadBackendServerException(
                                    "Kall til harSoknad feilet med HTTP-${httpResponse.statusCode.value()} for sykmelding $sykmeldingUuid",
                                )
                            }
                        }

                    throw exception.also {
                        log.error(it.message)
                    }
                }.toEntity<HarSoknadResponse>()

        return response.body?.harSoknad
            ?: throw RuntimeException("harSoknad response inneholdt ikke data for sykmelding $sykmeldingUuid")
    }

    @Retryable(exclude = [SykepengesoknadBackendClientException::class])
    override fun opprettOptIn(sykmeldingKafkaMessage: SykmeldingKafkaMessage) {
        sykepengesoknadBackendRestClient
            .post()
            .uri("/api/v2/soknader/opprett-opt-in")
            .body(sykmeldingKafkaMessage)
            .retrieve()
            .onStatus(HttpStatusCode::isError) { _, httpResponse ->
                val exception =
                    when {
                        httpResponse.statusCode.is4xxClientError -> {
                            SykepengesoknadBackendClientException(
                                "Kall til opprett-opt-in feilet med HTTP-${httpResponse.statusCode.value()}",
                            )
                        }
                        else -> {
                            SykepengesoknadBackendServerException(
                                "Kall til opprett-opt-in feilet med HTTP-${httpResponse.statusCode.value()}",
                            )
                        }
                    }

                throw exception.also {
                    log.error(it.message)
                }
            }
            .toEntity<Unit>()
    }
}

data class HarSoknadResponse(
    val harSoknad: Boolean,
)

class SykepengesoknadBackendClientException(
    message: String,
) : RuntimeException(message)

class SykepengesoknadBackendServerException(
    message: String,
) : RuntimeException(message)
