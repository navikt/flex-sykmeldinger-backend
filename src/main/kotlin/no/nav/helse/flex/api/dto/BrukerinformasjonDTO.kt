package no.nav.helse.flex.api.dto

data class BrukerinformasjonDTO(
    val arbeidsgivere: List<ArbeidsgiverDetaljerDTO>,
    val erOverSyttiAar: Boolean,
)

data class ArbeidsgiverDetaljerDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val navn: String,
    val aktivtArbeidsforhold: Boolean,
    val naermesteLeder: NarmesteLederDTO?,
)

data class NarmesteLederDTO(
    val navn: String,
    val orgnummer: String,
)
