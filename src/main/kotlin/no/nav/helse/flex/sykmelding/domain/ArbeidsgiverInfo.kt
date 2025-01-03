package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonSubTypes.Type
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id
import java.time.LocalDate

enum class ArbeidsgiverType {
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    INGEN_ARBEIDSGIVER,
}

@JsonSubTypes(
    Type(EnArbeidsgiver::class, name = "EN_ARBEIDSGIVER"),
    Type(FlereArbeidsgivere::class, name = "FLERE_ARBEIDSGIVERE"),
    Type(IngenArbeidsgiver::class, name = "INGEN_ARBEIDSGIVER"),
)
@JsonTypeInfo(use = Id.NAME, include = PROPERTY, property = "type")
sealed interface ArbeidsgiverInfo {
    val type: ArbeidsgiverType
}

data class EnArbeidsgiver(
    val meldingTilArbeidsgiver: String?,
    val tiltakArbeidsplassen: String?,
) : ArbeidsgiverInfo {
    override val type: ArbeidsgiverType = ArbeidsgiverType.EN_ARBEIDSGIVER
}

data class FlereArbeidsgivere(
    val navn: String?,
    val yrkesbetegnelse: String?,
    val stillingsprosent: Int?,
    val meldingTilArbeidsgiver: String?,
    val tiltakArbeidsplassen: String?,
) : ArbeidsgiverInfo {
    override val type: ArbeidsgiverType = ArbeidsgiverType.FLERE_ARBEIDSGIVERE
}

class IngenArbeidsgiver() : ArbeidsgiverInfo {
    override val type: ArbeidsgiverType = ArbeidsgiverType.INGEN_ARBEIDSGIVER

    override fun equals(other: Any?) = other is IngenArbeidsgiver

    override fun hashCode() = type.hashCode()

    override fun toString() = "IngenArbeidsgiver(type=$type)"
}

enum class IArbeidType {
    ER_I_ARBEID,
    ER_IKKE_I_ARBEID,
}

@JsonSubTypes(
    Type(ErIArbeid::class, name = "ER_I_ARBEID"),
    Type(ErIkkeIArbeid::class, name = "ER_IKKE_I_ARBEID"),
)
@JsonTypeInfo(use = Id.NAME, include = PROPERTY, property = "type")
sealed interface IArbeid {
    val type: IArbeidType
    val vurderingsdato: LocalDate?
}

data class ErIArbeid(
    val egetArbeidPaSikt: Boolean,
    val annetArbeidPaSikt: Boolean,
    val arbeidFOM: LocalDate?,
    override val vurderingsdato: LocalDate?,
) : IArbeid {
    override val type = IArbeidType.ER_I_ARBEID
}

data class ErIkkeIArbeid(
    val arbeidsforPaSikt: Boolean,
    val arbeidsforFOM: LocalDate?,
    override val vurderingsdato: LocalDate?,
) : IArbeid {
    override val type = IArbeidType.ER_IKKE_I_ARBEID
}
