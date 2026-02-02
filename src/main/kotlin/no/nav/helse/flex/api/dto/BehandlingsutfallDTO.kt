package no.nav.helse.flex.api.dto

data class BehandlingsutfallDTO(
    val status: RegelStatusDTO,
    val ruleHits: List<RegelinfoDTO>,
    val erUnderBehandling: Boolean = false,
)
