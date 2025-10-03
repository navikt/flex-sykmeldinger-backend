package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.EksternSykmeldingMelding
import no.nav.helse.flex.sykmelding.tsm.ISykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.tsm.ValidationResult

fun lagSykmeldingKafkaRecord(
    sykmelding: ISykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
    validation: ValidationResult = lagValidation(),
) = EksternSykmeldingMelding(
    sykmelding = sykmelding,
    validation = validation,
)
