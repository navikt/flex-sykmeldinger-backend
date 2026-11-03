package no.nav.helse.flex.gateways.aareg

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.gateways.aareg.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AaregDomeneTest {
    @Nested
    inner class ArbeidsstedTest {
        @Test
        fun `burde finne orgnummer med én ident`() {
            val arbeidssted =
                Arbeidssted(
                    type = ArbeidsstedType.Underenhet,
                    identer =
                        listOf(
                            Ident(
                                type = IdentType.ORGANISASJONSNUMMER,
                                ident = "orgnummer",
                            ),
                        ),
                )
            arbeidssted.finnOrgnummer() shouldBe "orgnummer"
        }

        @Test
        fun `burde ikke finne orgnummer dersom feil ident type`() {
            val arbeidssted =
                Arbeidssted(
                    type = ArbeidsstedType.Underenhet,
                    identer =
                        listOf(
                            Ident(
                                type = IdentType.FOLKEREGISTERIDENT,
                                ident = "orgnummer",
                            ),
                        ),
                )
            shouldThrow<Exception> {
                arbeidssted.finnOrgnummer() shouldBe "orgnummer"
            }
        }
    }

    @Nested
    inner class OpplysningspliktigTest {
        @Test
        fun `burde finne orgnummer med én ident`() {
            val opplysningspliktig =
                Opplysningspliktig(
                    type = "_",
                    identer =
                        listOf(
                            Ident(
                                type = IdentType.ORGANISASJONSNUMMER,
                                ident = "orgnummer",
                            ),
                        ),
                )
            opplysningspliktig.finnOrgnummer() shouldBe "orgnummer"
        }

        @Test
        fun `burde ikke finne orgnummer dersom feil ident type`() {
            val opplysningspliktig =
                Opplysningspliktig(
                    type = "_",
                    identer =
                        listOf(
                            Ident(
                                type = IdentType.FOLKEREGISTERIDENT,
                                ident = "orgnummer",
                            ),
                        ),
                )
            shouldThrow<Exception> {
                opplysningspliktig.finnOrgnummer() shouldBe "orgnummer"
            }
        }
    }
}
