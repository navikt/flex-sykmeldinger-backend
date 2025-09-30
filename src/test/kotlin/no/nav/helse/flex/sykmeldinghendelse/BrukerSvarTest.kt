package no.nav.helse.flex.sykmeldinghendelse

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.utils.objectMapper
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

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
        sporsmalSvar `should be equal to` deserialisert
    }
}
