package no.nav.helse.flex.api.dto

data class BehandlerDTO(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val adresse: no.nav.helse.flex.api.dto.AdresseDTO,
    val tlf: String?,
)
