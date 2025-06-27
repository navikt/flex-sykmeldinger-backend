package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.application.*
import java.time.LocalDate

fun lagArbeidstakerBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.ARBEIDSTAKER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    arbeidsgiverOrgnummer: SporsmalSvar<String> = lagSporsmalSvar("test-orgnummer"),
    riktigNarmesteLeder: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harEgenmeldingsdager: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): ArbeidstakerBrukerSvar =
    ArbeidstakerBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
        riktigNarmesteLeder = riktigNarmesteLeder,
        harEgenmeldingsdager = harEgenmeldingsdager,
        egenmeldingsdager = egenmeldingsdager,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagArbeidsledigBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.ARBEIDSLEDIG),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): ArbeidsledigBrukerSvar =
    ArbeidsledigBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagPermittertBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.PERMITTERT),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): PermittertBrukerSvar =
    PermittertBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagFiskerHyreBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.FISKER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    lottOgHyre: SporsmalSvar<FiskerLottOgHyre> = lagSporsmalSvar(FiskerLottOgHyre.HYRE),
    blad: SporsmalSvar<FiskerBlad> = lagSporsmalSvar(FiskerBlad.A),
    arbeidsgiverOrgnummer: SporsmalSvar<String> = lagSporsmalSvar("test-orgnummer"),
    riktigNarmesteLeder: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harEgenmeldingsdager: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): FiskerBrukerSvar =
    FiskerBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
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
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.FISKER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    lottOgHyre: SporsmalSvar<FiskerLottOgHyre> = lagSporsmalSvar(FiskerLottOgHyre.LOTT),
    blad: SporsmalSvar<FiskerBlad> = lagSporsmalSvar(FiskerBlad.A),
    harBruktEgenmelding: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): FiskerBrukerSvar =
    FiskerBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        lottOgHyre = lottOgHyre,
        blad = blad,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagFrilanserBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.FRILANSER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harBruktEgenmelding: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): FrilanserBrukerSvar =
    FrilanserBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagJordbrukerBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.JORDBRUKER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harBruktEgenmelding: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): JordbrukerBrukerSvar =
    JordbrukerBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagNaringsdrivendeBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.NAERINGSDRIVENDE),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harBruktEgenmelding: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): NaringsdrivendeBrukerSvar =
    NaringsdrivendeBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagAnnetArbeidssituasjonBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.ANNET),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): AnnetArbeidssituasjonBrukerSvar =
    AnnetArbeidssituasjonBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagUtdatertFormatBrukerSvar(
    arbeidssituasjon: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.ARBEIDSTAKER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    arbeidsgiverOrgnummer: SporsmalSvar<String>? = null,
    riktigNarmesteLeder: SporsmalSvar<Boolean>? = null,
    harEgenmeldingsdager: SporsmalSvar<Boolean>? = null,
    egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    harBruktEgenmelding: SporsmalSvar<Boolean>? = null,
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean>? = null,
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): UtdatertFormatBrukerSvar =
    UtdatertFormatBrukerSvar(
        arbeidssituasjon = arbeidssituasjon,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
        arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
        riktigNarmesteLeder = riktigNarmesteLeder,
        harEgenmeldingsdager = harEgenmeldingsdager,
        egenmeldingsdager = egenmeldingsdager,
    )

fun <T> lagSporsmalSvar(
    svar: T,
    sporsmaltekst: String = "<ukjent sporsmal>",
): SporsmalSvar<T> = SporsmalSvar(sporsmaltekst = sporsmaltekst, svar = svar)
