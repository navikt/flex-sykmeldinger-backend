package no.nav.helse.flex.api.dto

data class RegelinfoDTO(
    val messageForSender: String,
    val messageForUser: String,
    val ruleName: String,
    val ruleStatus: RegelStatusDTO?,
)

enum class RuleNameDTO {
    BEHANDLER_IKKE_GYLDIG_I_HPR,
    BEHANDLER_MANGLER_AUTORISASJON_I_HPR,
    BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR,
    BEHANDLER_MT_FT_KI_OVER_12_UKER,
    BEHANDLER_SUSPENDERT,
    PASIENT_ELDRE_ENN_70,
    ICPC_2_Z_DIAGNOSE,
    GRADERT_UNDER_20_PROSENT,
}
