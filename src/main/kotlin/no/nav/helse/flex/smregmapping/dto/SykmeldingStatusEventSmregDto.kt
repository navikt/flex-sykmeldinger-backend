package no.nav.helse.flex.smregmapping.dto

import java.time.LocalDate
import java.time.OffsetDateTime

data class SykmeldingStatusEventSmregDto(
    val sykmeldingId: String,
    val timestamp: OffsetDateTime,
    val statusEvent: String,
    val arbeidsgiver: ArbeidsgiverStatusKafkaDTO? = null,
    val sporsmals: List<SporsmalOgSvarKafkaDTO>? = null,
    val erSvarOppdatering: Boolean? = null,
    val tidligereArbeidsgiver: TidligereArbeidsgiverKafkaDTO? = null,
    val brukerSvar: KomplettInnsendtSkjemaSvar?,
)

const val STATUS_APEN = "APEN"
const val STATUS_AVBRUTT = "AVBRUTT"
const val STATUS_UTGATT = "UTGATT"
const val STATUS_SENDT = "SENDT"
const val STATUS_BEKREFTET = "BEKREFTET"
const val STATUS_SLETTET = "SLETTET"

data class ArbeidsgiverStatusKafkaDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String? = null,
    val orgNavn: String,
)

data class SporsmalOgSvarKafkaDTO(
    val tekst: String,
    val shortName: ShortNameKafkaDTO,
    val svartype: SvartypeKafkaDTO,
    val svar: String,
)

enum class ShortNameKafkaDTO {
    ARBEIDSSITUASJON,
    NY_NARMESTE_LEDER,
    FRAVAER,
    PERIODE,
    FORSIKRING,
    EGENMELDINGSDAGER,
}

enum class SvartypeKafkaDTO {
    ARBEIDSSITUASJON,
    PERIODER,
    JA_NEI,
    DAGER,
}

data class TidligereArbeidsgiverKafkaDTO(
    val orgNavn: String,
    val orgnummer: String,
    val sykmeldingsId: String,
)

data class KomplettInnsendtSkjemaSvar(
    val erOpplysningeneRiktige: SporsmalSvar<JaEllerNei>,
    val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysningerType>>?,
    val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>?,
    val riktigNarmesteLeder: SporsmalSvar<JaEllerNei>?,
    val harBruktEgenmelding: SporsmalSvar<JaEllerNei>?,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>?,
    val harForsikring: SporsmalSvar<JaEllerNei>?,
    val egenmeldingsdager: SporsmalSvar<List<LocalDate>>?,
    val harBruktEgenmeldingsdager: SporsmalSvar<JaEllerNei>?,
    val fisker: FiskereSvarKafkaDTO?,
)

data class FiskereSvarKafkaDTO(
    val blad: SporsmalSvar<Blad>,
    val lottOgHyre: SporsmalSvar<LottOgHyre>,
)

data class SporsmalSvar<T>(
    val sporsmaltekst: String,
    val svar: T,
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

data class Egenmeldingsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
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
