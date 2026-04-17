package no.nav.helse.flex.tsmsykmeldingstatus.dto

import no.nav.helse.flex.api.dto.*
import java.time.LocalDate

data class BrukerSvarKafkaDTO(
    val erOpplysningeneRiktige: FormSporsmalSvar<JaEllerNei>,
    val uriktigeOpplysninger: FormSporsmalSvar<List<UriktigeOpplysningerType>>?,
    val arbeidssituasjon: FormSporsmalSvar<ArbeidssituasjonDTO>,
    val arbeidsgiverOrgnummer: FormSporsmalSvar<String>?,
    val riktigNarmesteLeder: FormSporsmalSvar<JaEllerNei>?,
    val harBruktEgenmelding: FormSporsmalSvar<JaEllerNei>?,
    val egenmeldingsperioder: FormSporsmalSvar<List<EgenmeldingsperiodeFormDTO>>?,
    val sykFoerSykmeldingen: FormSporsmalSvar<JaEllerNei>?,
    val harForsikring: FormSporsmalSvar<JaEllerNei>?,
    val egenmeldingsdager: FormSporsmalSvar<List<LocalDate>>?,
    val harBruktEgenmeldingsdager: FormSporsmalSvar<JaEllerNei>?,
    val fisker: FiskereSvarKafkaDTO?,
)

data class SporsmalSvarKafkaDTO<T>(
    val sporsmaltekst: String,
    val svar: T,
)

enum class JaEllerNeiKafkaDTO {
    JA,
    NEI,
}

enum class UriktigeOpplysningerTypeKafkaDTO {
    PERIODE,
    SYKMELDINGSGRAD_FOR_HOY,
    SYKMELDINGSGRAD_FOR_LAV,
    ARBEIDSGIVER,
    DIAGNOSE,
    ANDRE_OPPLYSNINGER,
}

enum class ArbeidssituasjonKafkaDTO {
    ARBEIDSTAKER,
    FRILANSER,
    NAERINGSDRIVENDE,
    FISKER,
    JORDBRUKER,
    ARBEIDSLEDIG,
    ANNET,
}

data class EgenmeldingsperiodeKafkaDTO(
    val fom: LocalDate,
    val tom: LocalDate,
)

data class FiskereSvarKafkaDTO(
    val blad: FormSporsmalSvar<Blad>,
    val lottOgHyre: FormSporsmalSvar<LottOgHyre>,
)
