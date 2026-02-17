package no.nav.helse.flex.sykmeldingbekreftelse

import java.time.LocalDate

enum class ArbeidssituasjonDto {
    ARBEIDSTAKER_ORDINAER,
    ARBEIDSTAKER_FISKER_HYRE,
    NAERINGSDRIVENDE_ORDINAER,
    NAERINGSDRIVENDE_FISKER_LOTT,
    NAERINGSDRIVENDE_JORDBRUKER,
    FRILANSER_ORDINAER,
    ARBEIDSLEDIG_ORDINAER,
    ARBEIDSLEDIG_PERMITTERT, // TODO: Hvilken yrkesgruppe er PERMITTERT?
    FISKER_LOTT_OG_HYRE,
    UKJENT,
}

sealed interface BrukersituasjonDto {
    val arbeidssituasjon: ArbeidssituasjonDto
}

data class ArbeidstakerOrdinaerDto(
    val arbeidsgiver: ArbeidsgiverDto,
    val harRiktigNarmesteLeder: Boolean,
    val egenmeldingsdager: List<LocalDate>,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.ARBEIDSTAKER_ORDINAER
}

data class ArbeidstakerFiskerHyreDto(
    val arbeidsgiver: ArbeidsgiverDto,
    val harRiktigNarmesteLeder: Boolean,
    val egenmeldingsdager: List<LocalDate>,
    val fikserBlad: FiskerBlad,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.ARBEIDSTAKER_FISKER_HYRE
}

data class NaeringsdrivendeOrdinaerDto(
    val meldingTilNavPeriode: MeldingTilNavPeriodeDto? = null,
    val harForsikringForste16Dager: Boolean = false,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.NAERINGSDRIVENDE_ORDINAER
}

data class NaeringsdrivendeFiskerLottDto(
    val meldingTilNavPeriode: MeldingTilNavPeriodeDto? = null,
    val fikserBlad: FiskerBlad,
    val harForsikringForste16Dager: Boolean? = null,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.NAERINGSDRIVENDE_FISKER_LOTT
}

data class NaeringsdrivendeJordbrukerDto(
    val meldingTilNavPeriode: MeldingTilNavPeriodeDto? = null,
    val harForsikringForste16Dager: Boolean? = null,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.NAERINGSDRIVENDE_JORDBRUKER
}

data class FrilanserOrdinaerDto(
    val meldingTilNavPeriode: MeldingTilNavPeriodeDto? = null,
    val harForsikringForste16Dager: Boolean? = null,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.FRILANSER_ORDINAER
}

data class ArbeidsledigOrdinaerDto(
    val tidligereArbeidsgiver: ArbeidsgiverDto?,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.ARBEIDSLEDIG_ORDINAER
}

data class ArbeidsledigPermittertDto(
    val arbeidsgiver: ArbeidsgiverDto?,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.ARBEIDSLEDIG_PERMITTERT
}

data class FiskerLottOgHyreSituasjonDto(
    val arbeidsgiver: ArbeidsgiverDto,
    val harRiktigNarmesteLeder: Boolean,
    val egenmeldingsdager: List<LocalDate> = emptyList(),
    val fiskerBlad: FiskerBlad,
) : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.FISKER_LOTT_OG_HYRE
}

data object UkjentSituasjonDto : BrukersituasjonDto {
    override val arbeidssituasjon = ArbeidssituasjonDto.UKJENT
}

data class ArbeidsgiverDto(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val orgnavn: String,
)

data class MeldingTilNavPeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
)

enum class FiskerBlad {
    A,
    B,
}
