package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.sykmelding.tsm.ISykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.tsm.ValidationResult

data class EksternSykmeldingMelding(
    val sykmelding: ISykmeldingGrunnlag,
    val validation: ValidationResult,
)
