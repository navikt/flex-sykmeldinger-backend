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
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
        riktigNarmesteLeder = riktigNarmesteLeder,
        harEgenmeldingsdager = harEgenmeldingsdager,
        egenmeldingsdager = egenmeldingsdager,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagArbeidsledigBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    arbeidsledigFraOrgnummer: String? = null,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): ArbeidsledigBrukerSvar =
    ArbeidsledigBrukerSvar(
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagPermittertBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    arbeidsledigFraOrgnummer: String? = null,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): PermittertBrukerSvar =
    PermittertBrukerSvar(
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
        uriktigeOpplysninger = uriktigeOpplysninger,
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
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        lottOgHyre = lottOgHyre,
        blad = blad,
        arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
        riktigNarmesteLeder = riktigNarmesteLeder,
        harEgenmeldingsdager = harEgenmeldingsdager,
        egenmeldingsdager = egenmeldingsdager,
        uriktigeOpplysninger = uriktigeOpplysninger,
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
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        lottOgHyre = lottOgHyre,
        blad = blad,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagFrilanserBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    harBruktEgenmelding: Boolean = false,
    egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    harForsikring: Boolean = false,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): FrilanserBrukerSvar =
    FrilanserBrukerSvar(
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagJordbrukerBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    harBruktEgenmelding: Boolean = false,
    egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    harForsikring: Boolean = false,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): JordbrukerBrukerSvar =
    JordbrukerBrukerSvar(
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagNaringsdrivendeBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    harBruktEgenmelding: Boolean = false,
    egenmeldingsperioder: List<Egenmeldingsperiode>? = null,
    harForsikring: Boolean = false,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): NaringsdrivendeBrukerSvar =
    NaringsdrivendeBrukerSvar(
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagAnnetArbeidssituasjonBrukerSvar(
    erOpplysningeneRiktige: Boolean = true,
    uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
): AnnetArbeidssituasjonBrukerSvar =
    AnnetArbeidssituasjonBrukerSvar(
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )
