package no.nav.helse.flex.narmesteleder.domain

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class NarmesteLederLeesah(
    val narmesteLederId: UUID,
    val fnr: String,
    val orgnummer: String,
    val narmesteLederFnr: String,
    val aktivFom: LocalDate,
    val aktivTom: LocalDate?,
    val timestamp: OffsetDateTime,
)
