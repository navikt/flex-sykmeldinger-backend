package no.nav.helse.flex.api.dto

data class SporsmalSvarDTO(
    val sporsmal: String?,
    val svar: String,
    val restriksjoner: List<SvarRestriksjonDTO>,
)
