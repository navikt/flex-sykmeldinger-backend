package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.sykmelding.tsm.SykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.tsm.ValidationResult

data class EksternSykmeldingMelding(
    val sykmelding: SykmeldingGrunnlag,
    val validation: ValidationResult,
)
