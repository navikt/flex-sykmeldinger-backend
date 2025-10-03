package no.nav.helse.flex.gateways.aareg

import no.nav.helse.flex.gateways.aareg.*
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldThrow
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
            arbeidssted.finnOrgnummer() `should be equal to` "orgnummer"
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
            invoking {
                arbeidssted.finnOrgnummer() `should be equal to` "orgnummer"
            } shouldThrow (Exception::class)
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
            opplysningspliktig.finnOrgnummer() `should be equal to` "orgnummer"
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
            invoking {
                opplysningspliktig.finnOrgnummer() `should be equal to` "orgnummer"
            } shouldThrow (Exception::class)
        }
    }
}
