package no.nav.helse.flex.api

import com.nimbusds.jwt.JWTParser
import no.nav.helse.flex.gateways.texas.TexasClient
import no.nav.helse.flex.utils.logger
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
        forventetRoles: List<Roles>,
    ): Boolean {
        val jwtClaimsSet = JWTParser.parse(token).jwtClaimsSet
        val roles =
            jwtClaimsSet
                .getStringListClaim("roles")
                .mapNotNull { it.asRolleOrNull() }

        return roles.any { it in forventetRoles }
    }

    private fun String.asRolleOrNull(): Roles? {
        val role = Roles.entries.firstOrNull { it.value == this }
        if (role == null) {
            log.warn("Ukjent rolle i JWT token: $this")
        }
        return role
    }
}

enum class Roles(
    val value: String,
) {
    ROLE_SYKEPENGESOKNAD_BACKEND("role-sykepengesoknad-backend"),
}
