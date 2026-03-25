package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.config.Roles
import no.nav.helse.flex.gateways.texas.TexasClient
import no.nav.helse.flex.gateways.texas.TexasResponse

class TexasClientFake(
    private val flexGroupId: String,
) : TexasClient {
    override fun introspect(
        identityProvider: String,
        token: String,
    ): TexasResponse =
        when (token) {
            "gyldig-token-uten-rolle" -> {
                TexasResponse(true)
            }

            "gyldig-token-annen-rolle" -> {
                TexasResponse(true, listOf(Roles.ROLE_ACCESS_AS_APPLICATION.value))
            }

            "gyldig-token-role-sykepengesoknad-backend" -> {
                TexasResponse(
                    true,
                    listOf(Roles.ROLE_SYKEPENGESOKNAD_BACKEND.value),
                )
            }

            "gyldig-token-flex-gruppe" -> {
                TexasResponse(
                    active = true,
                    groups = listOf(flexGroupId),
                    NAVident = "A123456",
                )
            }

            "gyldig-token-annen-gruppe" -> {
                TexasResponse(
                    active = true,
                    groups = listOf("annen-gruppe"),
                    NAVident = "A123456",
                )
            }

            "ikke-gyldig-token" -> {
                TexasResponse(false)
            }

            else -> {
                TexasResponse(false)
            }
        }
}
