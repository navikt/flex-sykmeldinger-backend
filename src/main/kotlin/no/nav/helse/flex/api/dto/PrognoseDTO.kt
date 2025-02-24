package no.nav.helse.flex.api.dto

data class PrognoseDTO(
    val arbeidsforEtterPeriode: Boolean,
    val hensynArbeidsplassen: String?,
    val erIArbeid: no.nav.helse.flex.api.dto.ErIArbeidDTO?,
    val erIkkeIArbeid: no.nav.helse.flex.api.dto.ErIkkeIArbeidDTO?,
)
