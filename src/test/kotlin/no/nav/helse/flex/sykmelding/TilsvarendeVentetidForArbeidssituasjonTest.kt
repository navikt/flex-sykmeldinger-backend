package no.nav.helse.flex.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.BrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.FiskerBlad
import no.nav.helse.flex.sykmeldinghendelse.FiskerLottOgHyre
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.testdata.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TilsvarendeVentetidForArbeidssituasjonTest {
    @Nested
    inner class Naeringsdrivende {
        @Test
        fun `NaringsdrivendeBrukerSvar matcher NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagNaringsdrivendeBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) shouldBe true
        }

        @Test
        fun `JordbrukerBrukerSvar matcher NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagJordbrukerBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) shouldBe true
        }

        @Test
        fun `FiskerLott blad A matcher NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.A)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) shouldBe true
        }

        @Test
        fun `FiskerLott blad B matcher ikke NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.B)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) shouldBe false
        }

        @Test
        fun `FiskerHyre matcher ikke NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagFiskerHyreBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) shouldBe false
        }

        @Test
        fun `ArbeidsledigBrukerSvar matcher ikke NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagArbeidsledigBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) shouldBe false
        }
    }

    @Nested
    inner class Fisker {
        @Test
        fun `FiskerLott blad A matcher FISKER`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.A)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) shouldBe true
        }

        @Test
        fun `FiskerLott blad B matcher ikke FISKER`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.B)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) shouldBe false
        }

        @Test
        fun `FiskerHyre matcher ikke FISKER`() {
            lagSykmelding(brukerSvar = lagFiskerHyreBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) shouldBe false
        }

        @Test
        fun `FiskerBegge matcher ikke FISKER`() {
            lagSykmelding(
                brukerSvar =
                    lagFiskerHyreBrukerSvar(
                        lottOgHyre = lagSporsmalSvar(FiskerLottOgHyre.BEGGE),
                    ),
            ).tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) shouldBe false
        }

        @Test
        fun `NaringsdrivendeBrukerSvar matcher FISKER`() {
            lagSykmelding(brukerSvar = lagNaringsdrivendeBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) shouldBe true
        }

        @Test
        fun `JordbrukerBrukerSvar matcher FISKER`() {
            lagSykmelding(brukerSvar = lagJordbrukerBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) shouldBe true
        }

        @Test
        fun `ArbeidsledigBrukerSvar matcher ikke FISKER`() {
            lagSykmelding(brukerSvar = lagArbeidsledigBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) shouldBe false
        }
    }

    @Nested
    inner class Frilanser {
        @Test
        fun `FrilanserBrukerSvar matcher FRILANSER`() {
            lagSykmelding(brukerSvar = lagFrilanserBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FRILANSER) shouldBe true
        }

        @Test
        fun `NaringsdrivendeBrukerSvar matcher ikke FRILANSER`() {
            lagSykmelding(brukerSvar = lagNaringsdrivendeBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FRILANSER) shouldBe false
        }
    }

    @Nested
    inner class Jordbruker {
        @Test
        fun `JordbrukerBrukerSvar matcher JORDBRUKER`() {
            lagSykmelding(brukerSvar = lagJordbrukerBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.JORDBRUKER) shouldBe true
        }

        @Test
        fun `Fisker lott blad a matcher JORDBRUKER`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.A)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.JORDBRUKER) shouldBe true
        }

        @Test
        fun `NaringsdrivendeBrukerSvar matcher JORDBRUKER`() {
            lagSykmelding(brukerSvar = lagNaringsdrivendeBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.JORDBRUKER) shouldBe true
        }

        @Test
        fun `FrilanserBrukerSvar matcher ikke JORDBRUKER`() {
            lagSykmelding(brukerSvar = lagFrilanserBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.JORDBRUKER) shouldBe false
        }
    }
}

private fun lagSykmelding(brukerSvar: BrukerSvar) =
    lagSykmelding(
        hendelser =
            listOf(
                lagSykmeldingHendelse(
                    status = HendelseStatus.SENDT_TIL_NAV,
                    brukerSvar = brukerSvar,
                ),
            ),
    )
