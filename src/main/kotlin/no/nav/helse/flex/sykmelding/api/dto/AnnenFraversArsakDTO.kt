package no.nav.helse.flex.sykmelding.api.dto

data class AnnenFraversArsakDTO(
    val beskrivelse: String?,
    val grunn: List<AnnenFraverGrunnDTO>,
)
