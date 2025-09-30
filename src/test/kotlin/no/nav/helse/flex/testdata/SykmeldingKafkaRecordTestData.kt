package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.SykmeldingKafkaRecord
import no.nav.helse.flex.sykmelding.tsm.ISykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.tsm.ValidationResult

fun lagSykmeldingKafkaRecord(
    sykmelding: ISykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
    validation: ValidationResult = lagValidation(),
) = SykmeldingKafkaRecord(
    sykmelding = sykmelding,
    validation = validation,
)
