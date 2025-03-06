package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.sykmelding.domain.tsm.ValidationResult
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun lagValidation(status: RuleType = RuleType.OK): ValidationResult =
    ValidationResult(
        status = status,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        rules = listOf(),
    )
