package no.nav.helse.flex.sykmelding.api.dto

import java.time.LocalDate

data class MedisinskVurderingDTO(
    val hovedDiagnose: DiagnoseDTO?,
    val biDiagnoser: List<DiagnoseDTO>,
    val annenFraversArsak: AnnenFraversArsakDTO?,
    val svangerskap: Boolean,
    val yrkesskade: Boolean,
    val yrkesskadeDato: LocalDate?,
)
