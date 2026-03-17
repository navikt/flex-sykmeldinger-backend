package no.nav.helse.flex.domain

import java.net.URI
import java.time.Instant

data class AuditEntry(
    val appNavn: String,
    val utførtAv: String,
    val oppslagPå: String,
    val eventType: EventType,
    val forespørselTillatt: Boolean,
    val oppslagUtførtTid: Instant,
    val beskrivelse: String,
    val requestUrl: URI,
    val requestMethod: String,
)

enum class EventType(
    val logString: String,
) {
    CREATE("audit:create"),
    READ("audit:access"),
    UPDATE("audit:update"),
    DELETE("audit:delete"),
}
