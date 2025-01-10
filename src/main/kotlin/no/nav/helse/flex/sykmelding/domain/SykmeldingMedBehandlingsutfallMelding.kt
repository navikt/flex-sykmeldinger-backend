package no.nav.helse.flex.sykmelding.domain

data class SykmeldingMedBehandlingsutfallMelding(
    val sykmelding: ISykmeldingGrunnlag,
    val validation: ValidationResult,
)
