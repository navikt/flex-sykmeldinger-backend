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

class SykepengesoknadBackendClientException(
    message: String,
) : RuntimeException(message)

class SykepengesoknadBackendServerException(
    message: String,
) : RuntimeException(message)
