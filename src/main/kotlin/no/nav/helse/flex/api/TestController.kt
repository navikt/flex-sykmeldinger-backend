package no.nav.helse.flex.api

import jakarta.websocket.server.PathParam
import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TestController(
    private val aaregClient: AaregClient,
    private val environmentToggles: EnvironmentToggles,
) {
    private val log = logger()

    @Unprotected
    @GetMapping("/api/v1/test/kall-aareg/:orgnummer")
    fun kallAareg(
        @PathParam("orgnummer") orgnummer: String? = null,
    ): ResponseEntity<String> {
        if (environmentToggles.isProduction()) {
            throw RuntimeException("Kan ikke kalle test endepunkt i produksjon")
        }
        if (orgnummer == null) {
            throw IllegalArgumentException("Orgnummer må spesifiseres")
        }
        val res = aaregClient.getArbeidsstedArbeidsforholdoversikt(arbeidsstedOrgnummer = orgnummer)
        log.info("Test kall til Aareg returnerte for org $orgnummer: \n${res.serialisertTilString()}")

        return ResponseEntity.ok("Kall til Aareg i test gjennomført, se log")
    }
}
