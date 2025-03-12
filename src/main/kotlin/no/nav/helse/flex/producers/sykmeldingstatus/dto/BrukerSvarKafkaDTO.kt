package no.nav.helse.flex.producers.sykmeldingstatus.dto

import java.time.LocalDate

data class BrukerSvarKafkaDTO(
    val erOpplysningeneRiktige: SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO>,
    val uriktigeOpplysninger: SporsmalSvarKafkaDTO<List<UriktigeOpplysningerTypeKafkaDTO>>?,
    val arbeidssituasjon: SporsmalSvarKafkaDTO<ArbeidssituasjonKafkaDTO>,
    val arbeidsgiverOrgnummer: SporsmalSvarKafkaDTO<String>?,
    val riktigNarmesteLeder: SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO>?,
    val harBruktEgenmelding: SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO>?,
    val egenmeldingsperioder: SporsmalSvarKafkaDTO<List<EgenmeldingsperiodeKafkaDTO>>?,
    val harForsikring: SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO>?,
    val egenmeldingsdager: SporsmalSvarKafkaDTO<List<LocalDate>>?,
    val harBruktEgenmeldingsdager: SporsmalSvarKafkaDTO<JaEllerNeiKafkaDTO>?,
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
    val blad: SporsmalSvarKafkaDTO<BladKafkaDTO>,
    val lottOgHyre: SporsmalSvarKafkaDTO<LottOgHyreKafkaDTO>,
)

enum class BladKafkaDTO {
    A,
    B,
}

enum class LottOgHyreKafkaDTO {
    LOTT,
    HYRE,
    BEGGE,
}
