package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.sykmelding.SykmeldingVentetidService.Companion.tilsvarendeVentetidForArbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.BrukerSvar
import no.nav.helse.flex.sykmeldinghendelse.FiskerBlad
import no.nav.helse.flex.sykmeldinghendelse.FiskerLottOgHyre
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TilsvarendeVentetidForArbeidssituasjonTest {
    @Nested
    inner class Naeringsdrivende {
        @Test
        fun `NaringsdrivendeBrukerSvar matcher NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagNaringsdrivendeBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) `should be equal to` true
        }

        @Test
        fun `FiskerLott blad A matcher NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.A)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) `should be equal to` true
        }

        @Test
        fun `FiskerLott blad B matcher ikke NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.B)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) `should be equal to` false
        }

        @Test
        fun `FiskerHyre matcher ikke NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagFiskerHyreBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) `should be equal to` false
        }

        @Test
        fun `ArbeidsledigBrukerSvar matcher ikke NAERINGSDRIVENDE`() {
            lagSykmelding(brukerSvar = lagArbeidsledigBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.NAERINGSDRIVENDE) `should be equal to` false
        }
    }

    @Nested
    inner class Fisker {
        @Test
        fun `FiskerLott blad A matcher FISKER`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.A)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) `should be equal to` true
        }

        @Test
        fun `FiskerLott blad B matcher ikke FISKER`() {
            lagSykmelding(brukerSvar = lagFiskerLottBrukerSvar(blad = lagSporsmalSvar(FiskerBlad.B)))
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) `should be equal to` false
        }

        @Test
        fun `FiskerHyre matcher ikke FISKER`() {
            lagSykmelding(brukerSvar = lagFiskerHyreBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) `should be equal to` false
        }

        @Test
        fun `FiskerBegge matcher ikke FISKER`() {
            lagSykmelding(
                brukerSvar =
                    lagFiskerHyreBrukerSvar(
                        lottOgHyre = lagSporsmalSvar(FiskerLottOgHyre.BEGGE),
                    ),
            ).tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) `should be equal to` false
        }

        @Test
        fun `NaringsdrivendeBrukerSvar matcher FISKER`() {
            lagSykmelding(brukerSvar = lagNaringsdrivendeBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) `should be equal to` true
        }

        @Test
        fun `ArbeidsledigBrukerSvar matcher ikke FISKER`() {
            lagSykmelding(brukerSvar = lagArbeidsledigBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FISKER) `should be equal to` false
        }
    }

    @Nested
    inner class Frilanser {
        @Test
        fun `FrilanserBrukerSvar matcher FRILANSER`() {
            lagSykmelding(brukerSvar = lagFrilanserBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FRILANSER) `should be equal to` true
        }

        @Test
        fun `NaringsdrivendeBrukerSvar matcher ikke FRILANSER`() {
            lagSykmelding(brukerSvar = lagNaringsdrivendeBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.FRILANSER) `should be equal to` false
        }
    }

    @Nested
    inner class Jordbruker {
        @Test
        fun `JordbrukerBrukerSvar matcher JORDBRUKER`() {
            lagSykmelding(brukerSvar = lagJordbrukerBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.JORDBRUKER) `should be equal to` true
        }

        @Test
        fun `FrilanserBrukerSvar matcher ikke JORDBRUKER`() {
            lagSykmelding(brukerSvar = lagFrilanserBrukerSvar())
                .tilsvarendeVentetidForArbeidssituasjon(Arbeidssituasjon.JORDBRUKER) `should be equal to` false
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
