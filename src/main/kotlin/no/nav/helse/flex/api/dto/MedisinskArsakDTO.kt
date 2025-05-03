package no.nav.helse.flex.api.dto

data class MedisinskArsakDTO(
    val beskrivelse: String?,
    val arsak: List<MedisinskArsakTypeDTO>,
)
