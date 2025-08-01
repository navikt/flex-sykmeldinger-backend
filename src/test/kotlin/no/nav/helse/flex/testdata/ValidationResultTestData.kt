package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.tsm.PendingRule
import no.nav.helse.flex.sykmelding.domain.tsm.Reason
import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.sykmelding.domain.tsm.ValidationResult
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
                rules =
                    listOf(
                        PendingRule(
                            name = "TILBAKEDATERING_UNDER_BEHANDLING",
                            timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC), // Timestamp for the rule
                            reason =
                                Reason(
                                    sykmeldt = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
                                    sykmelder = "Sykmeldingen er til manuell behandling",
                                ),
                        ),
                    ),
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
