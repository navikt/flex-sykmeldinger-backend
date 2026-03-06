package no.nav.helse.flex.config

import jakarta.servlet.http.HttpServletRequest
import no.nav.helse.flex.gateways.texas.TexasClient
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service

@Service
class TokenValideringService(
    private val texasClient: TexasClient,
) {
    private val log = logger()

    fun validerTokenOgRolle(
        token: String?,
        identityProvider: String,
        forventedeRoller: List<Roles>,
    ) {
        if (token == null) {
            throw Uautorisert("Fant ikke token i request")
        }

        val respons =
            texasClient.introspect(
                identityProvider = identityProvider,
                token = token,
            )

        if (!respons.active) {
            log.info(respons.error)
            throw Uautorisert("Ugyldig token, ${respons.error}")
        }

        val harRolleMedTilgang =
            respons.roles
                .map { it.asRolleOrNull() }
                .any { it in forventedeRoller }

        if (!harRolleMedTilgang) {
            throw IngenTilgang("Ingen av rollene ${respons.roles} er forventet ${forventedeRoller.joinToString()}")
        }
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
    ROLE_ACCESS_AS_APPLICATION("access_as_application"),
}

fun HttpServletRequest.getToken(): String? = this.getHeader("Authorization")?.removePrefix("Bearer ")
