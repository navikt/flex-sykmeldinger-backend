package no.nav.helse.flex.sykmelding.api.dto

data class BehandlingsutfallDTO(
    val status: RegelStatusDTO,
    val ruleHits: List<RegelinfoDTO>,
)
