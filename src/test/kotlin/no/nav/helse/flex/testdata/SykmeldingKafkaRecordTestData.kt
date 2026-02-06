package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.EksternSykmeldingMelding
import no.nav.helse.flex.sykmelding.tsm.SykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.tsm.ValidationResult

fun lagEksternSykmeldingMelding(
    sykmelding: SykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
    validation: ValidationResult = lagValidation(),
) = EksternSykmeldingMelding(
    sykmelding = sykmelding,
    validation = validation,
)
