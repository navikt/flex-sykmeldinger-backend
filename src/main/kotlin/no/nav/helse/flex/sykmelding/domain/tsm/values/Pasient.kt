package no.nav.helse.flex.sykmelding.domain.tsm.values

data class Pasient(
    val navn: Navn?,
    val navKontor: String?,
    val navnFastlege: String?,
    val fnr: String,
    val kontaktinfo: List<Kontaktinfo>,
)
