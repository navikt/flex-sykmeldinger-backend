package no.nav.helse.flex.optin

import java.time.Instant

data class OptIn(
    val sykmeldingId: String,
    val opprettet: Instant,
)
