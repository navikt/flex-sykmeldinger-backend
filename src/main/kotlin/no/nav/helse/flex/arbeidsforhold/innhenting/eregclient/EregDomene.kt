package no.nav.helse.flex.arbeidsforhold.innhenting.eregclient

data class Nokkelinfo(
    val navn: Navn,
)

data class Navn(
    val sammensattnavn: String,
)
