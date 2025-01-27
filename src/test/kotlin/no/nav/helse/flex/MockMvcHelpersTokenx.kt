package no.nav.helse.flex

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.util.*

fun MockOAuth2Server.tokenxToken(
    fnr: String,
    acrClaim: String = "idporten-loa-high",
    audience: String = "flex-sykmeldinger-backend-client-id",
    issuerId: String = "tokenx",
    clientId: String = "frontend-client-id",
    claims: Map<String, Any> =
        mapOf(
            "acr" to acrClaim,
            "idp" to "idporten",
            "client_id" to clientId,
            "pid" to fnr,
        ),
): String =
    this
        .issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = UUID.randomUUID().toString(),
                audience = listOf(audience),
                claims = claims,
                expiry = 3600,
            ),
        ).serialize()
