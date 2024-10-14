package no.nav.helse.flex.tokenx

import no.nav.helse.flex.exception.AbstractApiError
import no.nav.helse.flex.exception.LogLevel
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class TokenxValidering(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    @Value("\${DITT_SYKEFRAVAER_FRONTEND_CLIENT_ID}")
    private val dittSykefravaerFrontendClientId: String,
) {
    fun validerFraDittSykefravaer(): String {
        return tokenValidationContextHolder.validerTokenXClaims(dittSykefravaerFrontendClientId).fnr()
    }
}

fun TokenValidationContextHolder.validerTokenXClaims(vararg tillattClient: String): JwtTokenClaims {
    val context = this.getTokenValidationContext()
    val claims = context.getClaims(TOKENX)
    val clientId = claims.getStringClaim("client_id")

    if (!tillattClient.toList().contains(clientId)) {
        throw IngenTilgang("Uventet clientId: $clientId")
    }
    return claims
}

fun JwtTokenClaims.fnr(): String {
    return this.getStringClaim("pid")
}

const val TOKENX = "tokenx"

class IngenTilgang(override val message: String) : AbstractApiError(
    message = message,
    httpStatus = HttpStatus.FORBIDDEN,
    reason = "INGEN_TILGANG",
    loglevel = LogLevel.WARN,
)
