package no.nav.helse.flex.sykmelding.api.dto

data class AdresseDTO(
    val gate: String?,
    val postnummer: Int?,
    val kommune: String?,
    val postboks: String?,
    val land: String?,
)
