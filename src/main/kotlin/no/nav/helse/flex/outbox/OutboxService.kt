package no.nav.helse.flex.outbox

import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelsePubliserer
import no.nav.helse.flex.utils.fraPsqlJson
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.tilPsqlJson
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class OutboxService(
    private val outboxDbRepository: OutboxDbRepository,
    private val sykmeldingLeser: SykmeldingLeser,
    private val sykmeldingHendelsePubliserer: SykmeldingHendelsePubliserer,
) : OutboxPubliserer {
    private val logger = logger()

    override fun outboxSykmeldingHendelse(
        fnr: String,
        sykmeldingId: String,
        sykmeldingHendelseId: String,
    ) {
        val outboxRecord =
            OutboxDbRecord(
                type = OutboxType.SYKMELDING_HENDELSE,
                fnr = fnr,
                payload =
                    SykmeldingHendelsePayload(
                        sykmeldingId = sykmeldingId,
                        sykmeldingHendelseId = sykmeldingHendelseId,
                    ).tilPsqlJson(),
                opprettetTimestamp = Instant.now(),
                sendtTimestamp = null,
            )
        outboxDbRepository.save(outboxRecord)
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun sendUsendteForEldsteLedigeFnr() {
        val usendte = outboxDbRepository.finnOgLasUsendteForEldsteLedigeFnr()
        if (usendte.isEmpty()) {
            return
        }
        usendte.forEach { record ->
            sendOutboxRecord(record)
            outboxDbRepository.save(record.copy(sendtTimestamp = Instant.now()))
        }
    }

    private fun sendOutboxRecord(record: OutboxDbRecord) {
        when (record.type) {
            OutboxType.SYKMELDING_HENDELSE -> sendSykmeldingHendelse(record)
        }
    }

    private fun sendSykmeldingHendelse(record: OutboxDbRecord) {
        val payload =
            record.payload.fraPsqlJson<SykmeldingHendelsePayload>()
                ?: error("Mangler payload på outbox-rad ${record.id}")
        val sykmelding =
            sykmeldingLeser.hentSykmelding(payload.sykmeldingId)
                ?: error("Fant ikke sykmelding ${payload.sykmeldingId} for outbox-rad ${record.id}")
        val hendelse =
            sykmelding.hendelser.firstOrNull { it.databaseId == payload.sykmeldingHendelseId }
                ?: error("Fant ikke hendelse ${payload.sykmeldingHendelseId} på sykmelding ${payload.sykmeldingId}")

        sykmeldingHendelsePubliserer.publiserHendelse(
            fnr = record.fnr,
            sykmeldingId = sykmelding.sykmeldingId,
            sykmeldingHendelse = hendelse,
        )
        logger.info("Sendte outbox-rad ${record.id} for sykmelding ${payload.sykmeldingId}")
    }
}

class SykmeldingHendelsePayload(
    val sykmeldingId: String,
    val sykmeldingHendelseId: String,
)
