package no.nav.helse.flex.api.dto

import java.time.LocalDate

data class SykmeldingSporsmalSvarDto(
    val erOpplysningeneRiktige: FormSporsmalSvar<JaEllerNei>,
    val uriktigeOpplysninger: FormSporsmalSvar<List<UriktigeOpplysningerType>>?,
    val arbeidssituasjon: FormSporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: FormSporsmalSvar<String>?,
    val arbeidsledig: ArbeidsledigFraOrgnummer?,
    val riktigNarmesteLeder: FormSporsmalSvar<JaEllerNei>?,
    val harBruktEgenmelding: FormSporsmalSvar<JaEllerNei>?,
    val egenmeldingsperioder: FormSporsmalSvar<List<Egenmeldingsperiode>>?,
    val harForsikring: FormSporsmalSvar<JaEllerNei>?,
    val egenmeldingsdager: FormSporsmalSvar<List<LocalDate>>?,
    val harBruktEgenmeldingsdager: FormSporsmalSvar<JaEllerNei>?,
    val fisker: FiskerSvar?,
)

data class FormSporsmalSvar<T>(
    val sporsmaltekst: String,
    val svar: T,
)

data class ArbeidsledigFraOrgnummer(
    val arbeidsledigFraOrgnummer: FormSporsmalSvar<String>?,
)

data class FiskerSvar(
    val blad: FormSporsmalSvar<Blad>,
    val lottOgHyre: FormSporsmalSvar<LottOgHyre>,
)

enum class Blad {
    A,
    B,
}

enum class LottOgHyre {
    LOTT,
    HYRE,
    BEGGE,
}

data class Egenmeldingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
)

enum class JaEllerNei {
    JA,
    NEI,
}

enum class UriktigeOpplysningerType {
    PERIODE,
    SYKMELDINGSGRAD_FOR_HOY,
    SYKMELDINGSGRAD_FOR_LAV,
    ARBEIDSGIVER,
    DIAGNOSE,
    ANDRE_OPPLYSNINGER,
}

enum class Arbeidssituasjon {
    ARBEIDSTAKER,
    FRILANSER,
    NAERINGSDRIVENDE,
    FISKER,
    JORDBRUKER,
    ARBEIDSLEDIG,
    ANNET,
}
