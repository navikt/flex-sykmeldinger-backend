package no.nav.helse.flex.sykmeldingbekreftelse.tolags

import java.time.LocalDate

enum class YrkesgruppeDto {
    ARBEIDSTAKER,
    NAERINGSDRIVENDE,
    FRILANSER,
    ARBEIDSLEDIG,
    FISKER_LOTT_OG_HYRE,
    UKJENT,
}

sealed interface BrukersituasjonDto {
    val yrkesgruppe: YrkesgruppeDto
}

data class ArbeidstakerYrkesgruppeDto(
    val arbeidsgiver: ArbeidsgiverDto,
    val harRiktigNarmesteLeder: Boolean,
    val egenmeldingsdager: List<LocalDate>,
    val utdypendeSituasjon: ArbeidstakerUtdypningDto? = null,
) : BrukersituasjonDto {
    override val yrkesgruppe = YrkesgruppeDto.ARBEIDSTAKER
}

data class NaeringsdrivendYrkesgruppeDto(
    val meldingTilNavPeriode: MeldingTilNavPeriodeDto? = null,
    val harForsikringForste16Dager: Boolean = false,
    val utdypendeSituasjon: NaeringsdrivendeUtdypningDto? = null,
) : BrukersituasjonDto {
    override val yrkesgruppe = YrkesgruppeDto.NAERINGSDRIVENDE
}

data class FrilanserYrkesgruppeDto(
    val meldingTilNavPeriode: MeldingTilNavPeriodeDto? = null,
    val harForsikringForste16Dager: Boolean = false,
) : BrukersituasjonDto {
    override val yrkesgruppe = YrkesgruppeDto.FRILANSER
}

data class ArbeidsledigYrkesgruppeDto(
    val tidligereArbeidsgiver: ArbeidsgiverDto? = null,
    val utdypendeSituasjon: ArbeidsledigUtdypningDto? = null,
) : BrukersituasjonDto {
    override val yrkesgruppe = YrkesgruppeDto.ARBEIDSLEDIG
}

data class FiskerLottOgHyreYrkesgruppeDto(
    val arbeidsgiver: ArbeidsgiverDto,
    val egenmeldingsdager: List<LocalDate> = emptyList(),
    val harRiktigNarmesteLeder: Boolean = false,
    val fiskerBlad: FiskerBlad,
) : BrukersituasjonDto {
    override val yrkesgruppe = YrkesgruppeDto.FISKER_LOTT_OG_HYRE
}

data object UkjentYrkesgruppeDto : BrukersituasjonDto {
    override val yrkesgruppe = YrkesgruppeDto.UKJENT
}

enum class ArbeidstakerSituasjon {
    FISKER_HYRE,
}

interface ArbeidstakerUtdypningDto {
    val arbeidssituasjon: ArbeidstakerSituasjon
}

data class FiskerHyreDto(
    val fikserBlad: FiskerBlad,
) : ArbeidstakerUtdypningDto {
    override val arbeidssituasjon = ArbeidstakerSituasjon.FISKER_HYRE
}

enum class NaeringsdrivendeSituasjon {
    FISKER_LOTT,
    JORDBRUKER,
}

sealed interface NaeringsdrivendeUtdypningDto {
    val arbeidssituasjon: NaeringsdrivendeSituasjon
}

data class FiskerLottDto(
    val fikserBlad: FiskerBlad,
) : NaeringsdrivendeUtdypningDto {
    override val arbeidssituasjon = NaeringsdrivendeSituasjon.FISKER_LOTT
}

data object JordbrukerDto : NaeringsdrivendeUtdypningDto {
    override val arbeidssituasjon = NaeringsdrivendeSituasjon.JORDBRUKER
}

enum class ArbeidsledigSituasjon {
    PERMITTERT,
}

sealed interface ArbeidsledigUtdypningDto {
    val arbeidssituasjon: ArbeidsledigSituasjon
}

data object PermittertDto : ArbeidsledigUtdypningDto {
    override val arbeidssituasjon = ArbeidsledigSituasjon.PERMITTERT
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
