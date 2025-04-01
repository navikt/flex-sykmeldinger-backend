package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.application.*
import java.time.LocalDate

fun lagArbeidstakerBrukerSvar(
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.ARBEIDSTAKER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    arbeidsgiverOrgnummer: SporsmalSvar<String> = lagSporsmalSvar("test-orgnummer"),
    riktigNarmesteLeder: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harEgenmeldingsdager: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): ArbeidstakerBrukerSvar =
    ArbeidstakerBrukerSvar(
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
        riktigNarmesteLeder = riktigNarmesteLeder,
        harEgenmeldingsdager = harEgenmeldingsdager,
        egenmeldingsdager = egenmeldingsdager,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagArbeidsledigBrukerSvar(
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.ARBEIDSLEDIG),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): ArbeidsledigBrukerSvar =
    ArbeidsledigBrukerSvar(
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagPermittertBrukerSvar(
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.PERMITTERT),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): PermittertBrukerSvar =
    PermittertBrukerSvar(
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagFiskerHyreBrukerSvar(
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.FISKER),
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
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
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
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.FISKER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    lottOgHyre: SporsmalSvar<FiskerLottOgHyre> = lagSporsmalSvar(FiskerLottOgHyre.LOTT),
    blad: SporsmalSvar<FiskerBlad> = lagSporsmalSvar(FiskerBlad.A),
    harBruktEgenmelding: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): FiskerBrukerSvar =
    FiskerBrukerSvar(
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        lottOgHyre = lottOgHyre,
        blad = blad,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagFrilanserBrukerSvar(
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.FRILANSER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harBruktEgenmelding: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): FrilanserBrukerSvar =
    FrilanserBrukerSvar(
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagJordbrukerBrukerSvar(
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.JORDBRUKER),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harBruktEgenmelding: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): JordbrukerBrukerSvar =
    JordbrukerBrukerSvar(
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagNaringsdrivendeBrukerSvar(
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.NAERINGSDRIVENDE),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    harBruktEgenmelding: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>? = null,
    harForsikring: SporsmalSvar<Boolean> = lagSporsmalSvar(false),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): NaringsdrivendeBrukerSvar =
    NaringsdrivendeBrukerSvar(
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        harBruktEgenmelding = harBruktEgenmelding,
        egenmeldingsperioder = egenmeldingsperioder,
        harForsikring = harForsikring,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun lagAnnetArbeidssituasjonBrukerSvar(
    arbeidssituasjonSporsmal: SporsmalSvar<Arbeidssituasjon> = lagSporsmalSvar(Arbeidssituasjon.ANNET),
    erOpplysningeneRiktige: SporsmalSvar<Boolean> = lagSporsmalSvar(true),
    uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
): AnnetArbeidssituasjonBrukerSvar =
    AnnetArbeidssituasjonBrukerSvar(
        arbeidssituasjonSporsmal = arbeidssituasjonSporsmal,
        erOpplysningeneRiktige = erOpplysningeneRiktige,
        uriktigeOpplysninger = uriktigeOpplysninger,
    )

fun <T> lagSporsmalSvar(
    svar: T,
    sporsmaltekst: String = "<ukjent sporsmal>",
): SporsmalSvar<T> = SporsmalSvar(sporsmaltekst = sporsmaltekst, svar = svar)
