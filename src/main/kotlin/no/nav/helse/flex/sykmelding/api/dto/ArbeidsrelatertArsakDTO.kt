package no.nav.helse.flex.sykmelding.api.dto

data class ArbeidsrelatertArsakDTO(
    val beskrivelse: String?,
    val arsak: List<ArbeidsrelatertArsakTypeDTO>,
)
