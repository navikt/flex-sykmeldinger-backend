package no.nav.helse.flex.api.dto

import java.time.LocalDate

data class SykmeldingsperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: GradertDTO?,
    val behandlingsdager: Int?,
    val innspillTilArbeidsgiver: String?,
    val type: PeriodetypeDTO,
    val aktivitetIkkeMulig: no.nav.helse.flex.api.dto.AktivitetIkkeMuligDTO?,
    val reisetilskudd: Boolean,
)

enum class PeriodetypeDTO {
    AKTIVITET_IKKE_MULIG,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    GRADERT,
    REISETILSKUDD,
}
