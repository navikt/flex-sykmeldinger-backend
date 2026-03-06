package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.gateways.texas.TexasClient
import no.nav.helse.flex.gateways.texas.TexasResponse

class TexasClientFake : TexasClient {
    override fun introspect(
        identityProvider: String,
        token: String,
    ): TexasResponse =
        when (token) {
            "gyldig-token-uten-rolle" -> TexasResponse(true)
            "gyldig-token-role-sykepengesoknad-backend" ->
                TexasResponse(
                    true,
                    listOf("role-sykepengesoknad-backend"),
                )

            "ikke-gyldig-token" -> TexasResponse(false)
            else -> TexasResponse(false)
        }
}
