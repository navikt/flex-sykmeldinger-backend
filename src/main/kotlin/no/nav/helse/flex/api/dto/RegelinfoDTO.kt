package no.nav.helse.flex.api.dto

data class RegelinfoDTO(
    val messageForSender: String,
    val messageForUser: String,
    val ruleName: String,
    val ruleStatus: RegelStatusDTO?,
)
