package no.nav.helse.flex.api

import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class TestController(
    private val aaregClient: AaregClient,
    private val environmentToggles: EnvironmentToggles,
) {
    private val log = logger()

    @GetMapping("/api/v1/test/kall-aareg")
    fun kallAareg(): ResponseEntity<String> {
        if (environmentToggles.isProduction()) {
            throw RuntimeException("Kan ikke kalle test endepunkt i produksjon")
        }

        val testOrg = "214613182"
        val res = aaregClient.getArbeidsstedArbeidsforholdoversikt(arbeidsstedOrgnummer = testOrg)
        log.info("Test kall til Aareg returnerte for org $testOrg: \n${res.serialisertTilString()}")

        return ResponseEntity.ok("Kall til Aareg i test gjennomført, se log")
    }
}
