package no.nav.helse.flex.api

import com.nimbusds.jwt.JWTParser
import no.nav.helse.flex.gateways.texas.TexasClient
import no.nav.helse.flex.utils.LogMarker
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

        log.info("Roller etter introspection ${respons.roles}")

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

        val harRolleMedTilgang = roles.any { it in forventetRoles }

        if (!harRolleMedTilgang) {
            log.warn("Ingen av rollene $roles er forventet ${forventetRoles.joinToString()}")
            log.error(LogMarker.TEAM_LOG, "Claims: $jwtClaimsSet")
        }
        return harRolleMedTilgang
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
