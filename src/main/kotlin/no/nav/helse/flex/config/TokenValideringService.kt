package no.nav.helse.flex.config

import jakarta.servlet.http.HttpServletRequest
import no.nav.helse.flex.gateways.texas.TexasClient
import no.nav.helse.flex.gateways.texas.TexasResponse
import no.nav.helse.flex.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TokenValideringService(
    private val texasClient: TexasClient,
    @param:Value($$"${flex.group.id}")
    private val flexGroupId: String,
) {
    private val log = logger()

    fun validerTokenOgRolle(
        token: String?,
        identityProvider: String,
        forventedeRoller: List<Roles>,
    ) {
        validerTokenOgRolleOgHentRespons(token, identityProvider, forventedeRoller)
    }

    fun validerGruppeOgHentNavIdent(
        token: String?,
        identityProvider: String,
        forventetGruppe: String = flexGroupId,
    ): String {
        val respons = validerTokenOgHentRespons(token, identityProvider)
        if (!respons.groups.contains(forventetGruppe)) {
            throw IngenTilgang("Ingen av gruppene ${respons.groups} inneholder forventet gruppe $forventetGruppe")
        }
        return respons.NAVident ?: throw Uautorisert("Fant ikke NAVident i token")
    }

    private fun validerTokenOgRolleOgHentRespons(
        token: String?,
        identityProvider: String,
        forventedeRoller: List<Roles>,
    ): TexasResponse {
        val respons = validerTokenOgHentRespons(token, identityProvider)

        val harRolleMedTilgang =
            respons.roles
                .map { it.asRolleOrNull() }
                .any { it in forventedeRoller }

        if (!harRolleMedTilgang) {
            throw IngenTilgang("Ingen av rollene ${respons.roles} er forventet ${forventedeRoller.joinToString()}")
        }

        return respons
    }

    private fun validerTokenOgHentRespons(
        token: String?,
        identityProvider: String,
    ): TexasResponse {
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

        return respons
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
    ROLE_FLEX_INTERNAL_FRONTEND("role-flex-internal-frontend"),
}

fun HttpServletRequest.getToken(): String? = this.getHeader("Authorization")?.removePrefix("Bearer ")
