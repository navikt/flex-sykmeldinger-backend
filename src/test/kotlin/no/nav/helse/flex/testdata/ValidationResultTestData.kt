package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.tsm.InvalidRule
import no.nav.helse.flex.sykmelding.tsm.OKRule
import no.nav.helse.flex.sykmelding.tsm.PendingRule
import no.nav.helse.flex.sykmelding.tsm.Reason
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmelding.tsm.ValidationResult
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun lagValidation(status: RuleType = RuleType.OK): ValidationResult =
    when (status) {
        RuleType.OK -> {
            ValidationResult(
                status = status,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                rules =
                    listOf(
                        OKRule(
                            name = "TILBAKEDATERING_UNDER_BEHANDLING",
                            timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC),
                        ),
                        PendingRule(
                            name = "TILBAKEDATERING_UNDER_BEHANDLING",
                            timestamp = OffsetDateTime.now().minusDays(1).withOffsetSameInstant(ZoneOffset.UTC),
                            reason =
                                Reason(
                                    sykmeldt = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
                                    sykmelder = "Sykmeldingen er til manuell behandling",
                                ),
                        ),
                    ),
            )
        }

        RuleType.PENDING -> {
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC),
                rules =
                    listOf(
                        PendingRule(
                            name = "TILBAKEDATERING_UNDER_BEHANDLING",
                            timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC),
                            reason =
                                Reason(
                                    sykmeldt = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
                                    sykmelder = "Sykmeldingen er til manuell behandling",
                                ),
                        ),
                        PendingRule(
                            name = "TILBAKEDATERING_UGYLDIG_TILBAKEDATERING",
                            timestamp = OffsetDateTime.now().minusDays(1).withOffsetSameInstant(ZoneOffset.UTC),
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
                rules =
                    listOf(
                        InvalidRule(
                            name = "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER",
                            timestamp = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC),
                            reason =
                                Reason(
                                    sykmeldt = "Tilbakedatering krever flere opplysninger",
                                    sykmelder = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
                                ),
                        ),
                    ),
            )
        }
    }
