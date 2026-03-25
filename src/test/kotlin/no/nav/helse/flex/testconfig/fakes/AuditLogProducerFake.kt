package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.domain.AuditEntry
import no.nav.helse.flex.gateways.AuditLogProducer

class AuditLogProducerFake : AuditLogProducer {
    private val entries = mutableListOf<AuditEntry>()

    fun hentAuditEntries(): List<AuditEntry> = entries.toList()

    fun reset() {
        entries.clear()
    }

    override fun lagAuditLog(auditEntry: AuditEntry) {
        entries.add(auditEntry)
    }
}
