package no.nav.helse.flex.sykmelding.domain

data class SykmeldingMedBehandlingsutfall(
    val sykmelding: ISykmelding,
    val validation: ValidationResult,
)
