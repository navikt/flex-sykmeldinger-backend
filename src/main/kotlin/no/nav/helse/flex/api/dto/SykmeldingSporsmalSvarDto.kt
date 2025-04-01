package no.nav.helse.flex.api.dto

import java.time.LocalDate

data class SykmeldingSporsmalSvarDto(
    val erOpplysningeneRiktige: FormSporsmalSvar<JaEllerNei>,
    val arbeidssituasjon: FormSporsmalSvar<ArbeidssituasjonDTO>,
    val uriktigeOpplysninger: FormSporsmalSvar<List<UriktigeOpplysningerType>>? = null,
    val arbeidsgiverOrgnummer: FormSporsmalSvar<String>? = null,
    val arbeidsledig: ArbeidsledigFraOrgnummer? = null,
    val riktigNarmesteLeder: FormSporsmalSvar<JaEllerNei>? = null,
    val harBruktEgenmelding: FormSporsmalSvar<JaEllerNei>? = null,
    val egenmeldingsperioder: FormSporsmalSvar<List<EgenmeldingsperiodeFormDTO>>? = null,
    val harForsikring: FormSporsmalSvar<JaEllerNei>? = null,
    val egenmeldingsdager: FormSporsmalSvar<List<LocalDate>>? = null,
    val harBruktEgenmeldingsdager: FormSporsmalSvar<JaEllerNei>? = null,
    val fisker: FiskerSvar? = null,
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

data class EgenmeldingsperiodeFormDTO(
    val fom: LocalDate,
    val tom: LocalDate,
) {
    constructor(periode: Pair<LocalDate, LocalDate>) : this(fom = periode.first, tom = periode.second)
}

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

enum class ArbeidssituasjonDTO {
    ARBEIDSTAKER,
    FRILANSER,
    NAERINGSDRIVENDE,
    FISKER,
    JORDBRUKER,
    ARBEIDSLEDIG,
    PERMITTERT,
    ANNET,
}
