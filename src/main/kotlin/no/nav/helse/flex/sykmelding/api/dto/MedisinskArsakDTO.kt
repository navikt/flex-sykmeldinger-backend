package no.nav.helse.flex.sykmelding.api.dto

data class MedisinskArsakDTO(
    val beskrivelse: String?,
    val arsak: List<MedisinskArsakTypeDTO>,
)
