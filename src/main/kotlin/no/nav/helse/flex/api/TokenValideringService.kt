package no.nav.helse.flex.api

import com.nimbusds.jwt.JWTParser
import no.nav.helse.flex.gateways.texas.TexasClient
import no.nav.helse.flex.utils.logger
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.stereotype.Service

@Service
class TokenValideringService(
    private val texasClient: TexasClient,
) {
    private val log = logger()

    fun validerToken(
        token: String?,
        identityProvider: String,
    ): Boolean {
        if (token == null) {
            return false
        }

        val respons =
            texasClient.introspect(
                identityProvider = identityProvider,
                token = token,
            )

        if (!respons.active) {
            log.info(respons.error)
        }

        return respons.active
    }

    fun validerClientIdFraToken(
        token: String,
        forventetClientIder: List<String>,
    ): Boolean {
        val jwtClaimsSet = JWTParser.parse(token).jwtClaimsSet
        val clientIdFraToken =
            JwtTokenClaims(jwtClaimsSet)
                .getStringClaim("azp")
                ?: return false

        return clientIdFraToken in forventetClientIder
    }
}
