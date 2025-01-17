package no.nav.helse.flex.sykmelding.api.dto

data class DiagnoseDTO(
    val kode: String,
    val system: String,
    val tekst: String?,
)
