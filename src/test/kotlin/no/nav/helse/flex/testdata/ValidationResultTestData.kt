package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.tsm.InvalidRule
import no.nav.helse.flex.sykmelding.tsm.OKRule
import no.nav.helse.flex.sykmelding.tsm.PendingRule
import no.nav.helse.flex.sykmelding.tsm.Reason
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmelding.tsm.ValidationResult
import no.nav.helse.flex.sykmelding.tsm.ValidationType
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun lagValidation(status: RuleType = RuleType.OK): ValidationResult =
    when (status) {
        RuleType.OK -> {
            ValidationResult(
                status = status,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                rules = listOf(),
            )
        }

        RuleType.PENDING -> {
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC), // Use a relevant timestamp
                rules = listOf(lagValidationPendingRule()),
            )
        }

        RuleType.INVALID -> {
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                rules = listOf(),
            )
        }
    }

fun lagValidationOkRule(
    name: String = "TILBAKEDATERING_DELVIS_GODKJENT",
    timestamp: OffsetDateTime = OffsetDateTime.parse("2020-01-01T00:00:00+00:00"),
    validationType: ValidationType = ValidationType.MANUAL,
): OKRule =
    OKRule(
        name = name,
        timestamp = timestamp,
        validationType = validationType,
    )

fun lagValidationPendingRule(
    name: String = "TILBAKEDATERING_UNDER_BEHANDLING",
    timestamp: OffsetDateTime = OffsetDateTime.parse("2020-01-01T00:00:00+00:00"),
    reason: Reason =
        Reason(
            sykmeldt = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
            sykmelder = "Sykmeldingen er til manuell behandling",
        ),
): PendingRule =
    PendingRule(
        name = name,
        timestamp = timestamp,
        reason = reason,
    )

fun lagValidationInvalidRule(
    name: String = "TILBAKEDATERING_UGYLDIG_TILBAKEDATERING",
    timestamp: OffsetDateTime = OffsetDateTime.parse("2020-01-01T00:00:00+00:00"),
    validationType: ValidationType = ValidationType.AUTOMATIC,
    reason: Reason =
        Reason(
            sykmeldt = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
            sykmelder = "Sykmeldingen er til manuell behandling",
        ),
): InvalidRule =
    InvalidRule(
        name = name,
        timestamp = timestamp,
        validationType = validationType,
        reason = reason,
    )
