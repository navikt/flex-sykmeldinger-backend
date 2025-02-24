package no.nav.helse.flex.api.dto

data class BrukerinformasjonDTO(
    val arbeidsgivere: List<no.nav.helse.flex.api.dto.VirksomhetDTO>,
)

data class VirksomhetDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val navn: String,
    val aktivtArbeidsforhold: Boolean,
    val naermesteLeder: no.nav.helse.flex.api.dto.NarmesteLederDTO?,
)

data class NarmesteLederDTO(
    val navn: String,
    val orgnummer: String,
)
