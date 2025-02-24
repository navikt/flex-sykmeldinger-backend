package no.nav.helse.flex.api.dto

data class AnnenFraversArsakDTO(
    val beskrivelse: String?,
    val grunn: List<no.nav.helse.flex.api.dto.AnnenFraverGrunnDTO>,
)
