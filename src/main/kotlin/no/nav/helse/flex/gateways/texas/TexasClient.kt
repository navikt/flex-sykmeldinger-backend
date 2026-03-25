package no.nav.helse.flex.gateways.texas

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

data class TexasRequest(
    val identity_provider: String,
    val token: String,
)

data class TexasResponse(
    val active: Boolean,
    val roles: List<String> = emptyList(),
    val groups: List<String> = emptyList(),
    val error: String? = null,
    val NAVident: String? = null,
)

interface TexasClient {
    fun introspect(
        identityProvider: String,
        token: String,
    ): TexasResponse
}

@Component
class TexasEksternClient(
    private val texasRestClient: RestClient,
) : TexasClient {
    override fun introspect(
        identityProvider: String,
        token: String,
    ): TexasResponse =
        texasRestClient
            .post()
            .uri { uriBuilder -> uriBuilder.build() }
            .headers {
                it.contentType = MediaType.APPLICATION_JSON
            }.body(
                TexasRequest(
                    identity_provider = identityProvider,
                    token = token,
                ),
            ).retrieve()
            .toEntity<TexasResponse>()
            .body
            ?: throw RuntimeException("Texas introspection mangler body")
}
