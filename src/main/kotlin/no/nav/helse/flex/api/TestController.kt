package no.nav.helse.flex.api

import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class TestController(
    private val aaregClient: AaregClient,
    private val environmentToggles: EnvironmentToggles,
) {
    private val log = logger()

    @Unprotected
    @GetMapping("/api/v1/test/kall-aareg/{orgnummer}")
    fun kallAareg(
        @PathVariable("orgnummer") orgnummer: String,
    ): ResponseEntity<String> {
        if (environmentToggles.isProduction()) {
            throw RuntimeException("Kan ikke kalle test endepunkt i produksjon")
        }
        val res = aaregClient.getArbeidsstedArbeidsforholdoversikt(arbeidsstedOrgnummer = orgnummer)
        log.info("Test kall til Aareg returnerte for org: \n${res.serialisertTilString()}")

        return ResponseEntity.ok("Kall til Aareg i test gjennomført, se log")
    }
}
