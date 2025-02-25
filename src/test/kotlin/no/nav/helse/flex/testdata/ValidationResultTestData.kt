package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.sykmelding.domain.tsm.ValidationResult
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun lagValidation(): ValidationResult =
    ValidationResult(
        status = RuleType.OK,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        rules = listOf(),
    )
