package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.application.*
import java.time.LocalDate

fun lagArbeidstakerBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    arbeidsgiverOrgnummer: String = "test-orgnummer",
    riktigNarmesteLeder: Boolean = true,
    harEgenmeldingsdager: Boolean = false,
    egenmeldingsdager: List<LocalDate>? = null,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): ArbeidstakerBrukerSvar =
    ArbeidstakerBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.ARBEIDSTAKER.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        arbeidsgiverOrgnummer = arbeidsgiverOrgnummer.somUkjentSporsmal(),
        riktigNarmesteLeder = riktigNarmesteLeder.somUkjentSporsmal(),
        harEgenmeldingsdager = harEgenmeldingsdager.somUkjentSporsmal(),
        egenmeldingsdager = egenmeldingsdager?.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

fun lagArbeidsledigBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    arbeidsledigFraOrgnummer: String? = null,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): ArbeidsledigBrukerSvar =
    ArbeidsledigBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.ARBEIDSLEDIG.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer?.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

fun lagPermittertBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    arbeidsledigFraOrgnummer: String? = null,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): PermittertBrukerSvar =
    PermittertBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.PERMITTERT.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer?.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

fun lagFiskerHyreBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    lottOgHyre: FiskerLottOgHyre = FiskerLottOgHyre.HYRE,
    blad: FiskerBlad = FiskerBlad.A,
    arbeidsgiverOrgnummer: String = "test-orgnummer",
    riktigNarmesteLeder: Boolean = true,
    harEgenmeldingsdager: Boolean = false,
    egenmeldingsdager: List<LocalDate>? = null,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): FiskerBrukerSvar =
    FiskerBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.FISKER.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        lottOgHyre = lottOgHyre.somUkjentSporsmal(),
        blad = blad.somUkjentSporsmal(),
        arbeidsgiverOrgnummer = arbeidsgiverOrgnummer.somUkjentSporsmal(),
        riktigNarmesteLeder = riktigNarmesteLeder.somUkjentSporsmal(),
        harEgenmeldingsdager = harEgenmeldingsdager.somUkjentSporsmal(),
        egenmeldingsdager = egenmeldingsdager?.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

fun lagFiskerLottBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    lottOgHyre: FiskerLottOgHyre = FiskerLottOgHyre.LOTT,
    blad: FiskerBlad = FiskerBlad.A,
    harBruktEgenmelding: Boolean = false,
    egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    harForsikring: Boolean = false,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): FiskerBrukerSvar =
    FiskerBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.FISKER.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        lottOgHyre = lottOgHyre.somUkjentSporsmal(),
        blad = blad.somUkjentSporsmal(),
        harBruktEgenmelding = harBruktEgenmelding.somUkjentSporsmal(),
        egenmeldingsperioder = egenmeldingsperioder?.somUkjentSporsmal(),
        harForsikring = harForsikring.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

fun lagFrilanserBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    harBruktEgenmelding: Boolean = false,
    egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    harForsikring: Boolean = false,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): FrilanserBrukerSvar =
    FrilanserBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.FRILANSER.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        harBruktEgenmelding = harBruktEgenmelding.somUkjentSporsmal(),
        egenmeldingsperioder = egenmeldingsperioder?.somUkjentSporsmal(),
        harForsikring = harForsikring.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

fun lagJordbrukerBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    harBruktEgenmelding: Boolean = false,
    egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    harForsikring: Boolean = false,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): JordbrukerBrukerSvar =
    JordbrukerBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.JORDBRUKER.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        harBruktEgenmelding = harBruktEgenmelding.somUkjentSporsmal(),
        egenmeldingsperioder = egenmeldingsperioder?.somUkjentSporsmal(),
        harForsikring = harForsikring.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

fun lagNaringsdrivendeBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    harBruktEgenmelding: Boolean = false,
    egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    harForsikring: Boolean = false,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): NaringsdrivendeBrukerSvar =
    NaringsdrivendeBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.NAERINGSDRIVENDE.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        harBruktEgenmelding = harBruktEgenmelding.somUkjentSporsmal(),
        egenmeldingsperioder = egenmeldingsperioder?.somUkjentSporsmal(),
        harForsikring = harForsikring.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

fun lagAnnetArbeidssituasjonBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): AnnetArbeidssituasjonBrukerSvar =
    AnnetArbeidssituasjonBrukerSvar(
        arbeidssituasjonSporsmal = Arbeidssituasjon.ANNET.somUkjentSporsmal(),
        erOpplysningeneRiktige = erOpplysningeneRiktige.somUkjentSporsmal(),
        uriktigeOpplysninger = uriktigeOpplysninger?.somUkjentSporsmal(),
    )

private fun <T : Any> T.somUkjentSporsmal(): SporsmalSvar<T> = SporsmalSvar("<ukjent sporsmal>", this)
