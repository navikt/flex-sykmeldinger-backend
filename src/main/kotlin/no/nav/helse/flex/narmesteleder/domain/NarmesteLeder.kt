package no.nav.helse.flex.narmesteleder.domain

import org.springframework.data.annotation.Id
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class NarmesteLeder(
    @Id
    val id: String? = null,
    val oppdatert: Instant,
    val timestamp: Instant,
    val narmesteLederId: UUID,
    val orgnummer: String,
    val brukerFnr: String,
    val narmesteLederFnr: String,
    val aktivFom: LocalDate,
    val narmesteLederNavn: String?,
)
