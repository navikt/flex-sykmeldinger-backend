package no.nav.helse.flex.narmesteleder

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NarmesteLederLesemodusTest {
    @Test
    fun `ESYFO-lesing krever aktiv consumer`() {
        runCatching { NarmesteLederLesemodus("ESYFO", false) }.isFailure shouldBe true
        NarmesteLederLesemodus("GAMMEL", false).modus shouldBe "GAMMEL"
        NarmesteLederLesemodus("ESYFO", true).modus shouldBe "ESYFO"
        runCatching { NarmesteLederLesemodus("ukjent", true) }.isFailure shouldBe true
    }
}
