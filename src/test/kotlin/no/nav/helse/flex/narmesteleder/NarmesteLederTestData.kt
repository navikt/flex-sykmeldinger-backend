package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

fun lagNarmesteLeder(
    brukerFnr: String = "fnr",
    orgnummer: String = "123456789",
    aktivFom: LocalDate = LocalDate.parse("2022-02-02"),
    narmesteLederNavn: String = "Leder Navn",
    narmesteLederId: UUID = UUID.randomUUID(),
): NarmesteLeder =
    NarmesteLeder(
        oppdatert = Instant.parse("2022-02-02T00:00:00.00Z"),
        timestamp = Instant.parse("2022-02-02T00:00:00.00Z"),
        narmesteLederId = narmesteLederId,
        orgnummer = orgnummer,
        brukerFnr = brukerFnr,
        narmesteLederFnr = "lederFnr",
        aktivFom = aktivFom,
        narmesteLederNavn = narmesteLederNavn,
    )

fun lagNarmesteLederLeesah(
    narmesteLederId: UUID,
    orgnummer: String = "999999",
    aktivTom: LocalDate? = null,
): NarmesteLederLeesah =
    NarmesteLederLeesah(
        narmesteLederId = narmesteLederId,
        fnr = "12345678910",
        orgnummer = orgnummer,
        narmesteLederFnr = "01985554321",
        aktivFom = LocalDate.now(),
        aktivTom = aktivTom,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
    )
