package no.nav.helse.flex.sykmelding.api.dto

data class BehandlerDTO(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val adresse: AdresseDTO,
    val tlf: String?,
)
