package no.nav.helse.flex.sykmelding.api.dto

data class PasientDTO(
    val fnr: String? = null,
    val fornavn: String? = null,
    val mellomnavn: String? = null,
    val etternavn: String? = null,
    val overSyttiAar: Boolean? = null,
)
