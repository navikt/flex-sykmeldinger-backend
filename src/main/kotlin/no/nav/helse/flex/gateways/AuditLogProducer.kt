package no.nav.helse.flex.gateways

import no.nav.helse.flex.domain.AuditEntry
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

const val AUDIT_TOPIC = "flex.auditlogging"

interface AuditLogProducer {
    fun lagAuditLog(auditEntry: AuditEntry)
}

@Component
class AuditLogProducerImpl(
    @param:Qualifier("kafkaProducer") private val kafkaProducer: KafkaProducer<String, String>,
) : AuditLogProducer {
    private val log = logger()

    override fun lagAuditLog(auditEntry: AuditEntry) {
        try {
            kafkaProducer
                .send(
                    ProducerRecord(
                        AUDIT_TOPIC,
                        UUID.randomUUID().toString(),
                        auditEntry.serialisertTilString(),
                    ),
                ).get()
        } catch (e: Exception) {
            log.error("Klarte ikke publisere AuditEntry på kafka")
            throw e
        }
    }
}
