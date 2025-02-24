package no.nav.helse.flex.api.dto

data class ArbeidsrelatertArsakDTO(
    val beskrivelse: String?,
    val arsak: List<no.nav.helse.flex.api.dto.ArbeidsrelatertArsakTypeDTO>,
)
