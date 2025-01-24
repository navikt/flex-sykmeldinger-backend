package no.nav.helse.flex.sykmelding.api.dto

data class BrukerinformasjonDTO(
    val arbeidsgivere: List<VirksomhetDTO>,
)

data class VirksomhetDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val navn: String,
    val aktivtArbeidsforhold: Boolean,
    val naermesteLeder: NarmesteLederDTO?,
)

data class NarmesteLederDTO(
    val navn: String,
    val orgnummer: String,
    val organisasjonsnavn: String,
)
