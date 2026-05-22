package no.nav.helse.flex.gateways.ereg

data class Nokkelinfo(
    val navn: Navn,
)

data class Navn(
    val sammensattnavn: String,
)

data class HentOrganisasjonerRequest(
    val organisasjonsnummere: List<String>,
)

data class HentOrganisasjonerResponse(
    val organisasjoner: Map<String, OrganisasjonInfo>,
)

data class OrganisasjonInfo(
    val navn: Navn,
)
