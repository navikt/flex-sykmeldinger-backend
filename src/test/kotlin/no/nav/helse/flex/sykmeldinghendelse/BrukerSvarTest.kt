package no.nav.helse.flex.sykmeldinghendelse

import io.kotest.matchers.shouldBe
import no.nav.helse.flex.utils.objectMapper
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class BrukerSvarTest {
    @Test
    fun `burde deserialisere SporsmalSvar`() {
        val sporsmalSvar =
            SporsmalSvar(
                sporsmaltekst = "Hvordan har du det?",
                svar = "Bra",
            )
        val json = objectMapper.writeValueAsString(sporsmalSvar)
        val deserialisert: SporsmalSvar<String> = objectMapper.readValue(json)
        sporsmalSvar shouldBe deserialisert
    }
}
