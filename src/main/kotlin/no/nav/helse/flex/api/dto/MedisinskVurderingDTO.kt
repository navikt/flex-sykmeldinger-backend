package no.nav.helse.flex.api.dto

import java.time.LocalDate

data class MedisinskVurderingDTO(
    val hovedDiagnose: DiagnoseDTO?,
    val biDiagnoser: List<DiagnoseDTO>,
    val annenFraversArsak: no.nav.helse.flex.api.dto.AnnenFraversArsakDTO?,
    val svangerskap: Boolean,
    val yrkesskade: Boolean,
    val yrkesskadeDato: LocalDate?,
)
