package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.domain.tsm.ISykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.tsm.Meldingsinformasjon
import no.nav.helse.flex.sykmelding.domain.tsm.ValidationResult

data class SykmeldingKafkaRecord(
    val metadata: Meldingsinformasjon,
    val sykmelding: ISykmeldingGrunnlag,
    val validation: ValidationResult,
)
