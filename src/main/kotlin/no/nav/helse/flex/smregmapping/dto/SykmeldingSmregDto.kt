package no.nav.helse.flex.smregmapping.dto

import java.time.LocalDate
import java.time.OffsetDateTime

data class SykmeldingSmregDto(
    val id: String,
    val mottattTidspunkt: OffsetDateTime,
    val syketilfelleStartDato: LocalDate?,
    val behandletTidspunkt: OffsetDateTime,
    val signaturDato: OffsetDateTime?,
    val arbeidsgiver: ArbeidsgiverSmregDto,
    val sykmeldingsperioder: List<SykmeldingsperiodeSmregDto>,
    val prognose: PrognoseSmregDto?,
    val tiltakArbeidsplassen: String?,
    val meldingTilArbeidsgiver: String?,
    val kontaktMedPasient: KontaktMedPasientSmregDto,
    val behandler: BehandlerSmregDto?,
    val egenmeldt: Boolean,
    val papirsykmelding: Boolean,
    val harRedusertArbeidsgiverperiode: Boolean,
    val merknader: List<MerknadSmregDto>?,
    val utenlandskSykmelding: UtenlandskSykmeldingSmregDto?,
)

data class ArbeidsgiverSmregDto(
    val navn: String?,
    val yrkesbetegnelse: String?,
)

data class SykmeldingsperiodeSmregDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: GradertSmregDto?,
    val behandlingsdager: Int?,
    val innspillTilArbeidsgiver: String?,
    val type: PeriodetypeSmregDto,
    val aktivitetIkkeMulig: AktivitetIkkeMuligSmregDto?,
    val reisetilskudd: Boolean,
)

data class PrognoseSmregDto(
    val arbeidsforEtterPeriode: Boolean,
    val hensynArbeidsplassen: String?,
)

data class KontaktMedPasientSmregDto(
    val kontaktDato: LocalDate?,
)

data class BehandlerSmregDto(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val hpr: String?,
    val adresse: AdresseSmregDto,
    val tlf: String?,
)

data class MerknadSmregDto(
    val type: String,
    val beskrivelse: String?,
)

enum class MerknadtypeSmregDto {
    DELVIS_GODKJENT,
    TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER,
    UGYLDIG_TILBAKEDATERING,
    UNDER_BEHANDLING,
    TILBAKEDATERT_PAPIRSYKMELDING,
    UKJENT_MERKNAD,
}

data class UtenlandskSykmeldingSmregDto(
    val land: String,
)

data class AdresseSmregDto(
    val gate: String?,
    val postnummer: Int?,
    val kommune: String?,
    val postboks: String?,
    val land: String?,
)

enum class PeriodetypeSmregDto {
    AKTIVITET_IKKE_MULIG,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    GRADERT,
    REISETILSKUDD,
}

data class AktivitetIkkeMuligSmregDto(
    val arbeidsrelatertArsak: ArbeidsrelatertArsakSmregDto?,
)

data class ArbeidsrelatertArsakSmregDto(
    val beskrivelse: String?,
    val arsak: List<ArbeidsrelatertArsakTypeSmregDto>,
)

enum class ArbeidsrelatertArsakTypeSmregDto(
    val codeValue: String,
    val text: String,
    val oid: String = "2.16.578.1.12.4.1.1.8132",
) {
    MANGLENDE_TILRETTELEGGING("1", "Manglende tilrettelegging på arbeidsplassen"),
    ANNET("9", "Annet"),
}

data class GradertSmregDto(
    val grad: Int,
    val reisetilskudd: Boolean,
)
