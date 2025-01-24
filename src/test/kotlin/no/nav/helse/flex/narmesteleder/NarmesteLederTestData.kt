package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

fun lagNarmesteLeder(
    brukerFnr: String = "fnr",
    orgnummer: String = "123456789",
    aktivFom: LocalDate = LocalDate.parse("2022-02-02"),
): NarmesteLeder =
    NarmesteLeder(
        oppdatert = Instant.parse("2022-02-02T00:00:00.00Z"),
        timestamp = Instant.parse("2022-02-02T00:00:00.00Z"),
        narmesteLederId = UUID.fromString("00000000-0000-0000-0000-000000000000"),
        orgnummer = orgnummer,
        brukerFnr = brukerFnr,
        narmesteLederFnr = "lederFnr",
        aktivFom = aktivFom,
        narmesteLederNavn = "Leder Navn",
    )
