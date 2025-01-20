package no.nav.helse.flex.sykmelding.api.dto

data class SporsmalSvarDTO(
    val sporsmal: String?,
    val svar: String,
    val restriksjoner: List<SvarRestriksjonDTO>,
)
