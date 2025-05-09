package no.nav.helse.flex.tsmsykmeldingstatus

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.config.AdvisoryLock
import no.nav.helse.flex.producers.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.postgresql.util.PGobject
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingStatusBuffer(
    private val sykmeldingStatusBufferRepository: SykmeldingStatusBufferRepository,
    private val advisoryLock: AdvisoryLock,
    private val nowFactory: Supplier<Instant>,
) {
    private val log = logger()

    @Transactional(rollbackFor = [Exception::class])
    fun leggTil(hendelse: SykmeldingStatusKafkaMessageDTO) {
        val sykmeldingId = hendelse.kafkaMetadata.sykmeldingId
        log.info("Skal legge til sykmeldingstatus i buffer. Status: ${hendelse.event.statusEvent}, sykmeldingId: $sykmeldingId")
        val record = hendelse.tilBuffretSykmeldingHendelseDbRecord(now = nowFactory.get())
        aquireBufferLockFor(sykmeldingId)
        sykmeldingStatusBufferRepository.save(record)
        log.info("Lagt til sykmeldingstatus i buffer. Status: ${hendelse.event.statusEvent}, sykmeldingId: $sykmeldingId")
    }

    fun kikkPaaAlleFor(sykmeldingId: String): List<SykmeldingStatusKafkaMessageDTO> =
        sykmeldingStatusBufferRepository
            .findAllBySykmeldingId(sykmeldingId)
            .map { it.tilSykmeldingStatusKafkaMessageDTO() }

    @Transactional(rollbackFor = [Exception::class])
    fun prosesserAlleFor(sykmeldingId: String): List<SykmeldingStatusKafkaMessageDTO> {
        log.info("Skal prosesserer alle sykmeldingstatuser i buffer for sykmeldingId: $sykmeldingId")
        aquireBufferLockFor(sykmeldingId)
        val records = sykmeldingStatusBufferRepository.findAllBySykmeldingId(sykmeldingId)
        sykmeldingStatusBufferRepository.deleteAll(records)
        val statuses =
            records
                .sortedBy { it.sykmeldingStatusOpprettet }
                .map { it.tilSykmeldingStatusKafkaMessageDTO() }
        if (statuses.isEmpty()) {
            log.info("Ingen sykmeldingstatuser i buffer for sykmeldingId: $sykmeldingId")
        } else {
            log.info("Prosesserer ${statuses.size} sykmeldingstatuser i buffer for sykmeldingId: $sykmeldingId")
        }
        return statuses
    }

    private fun aquireBufferLockFor(sykmeldingId: String) {
        advisoryLock.acquire("SykmeldingHendelseBuffer", sykmeldingId)
    }

    companion object {
        internal fun SykmeldingStatusKafkaMessageDTO.tilBuffretSykmeldingHendelseDbRecord(now: Instant): SykmeldingStatusBufferDbRecord =
            SykmeldingStatusBufferDbRecord(
                sykmeldingId = this.kafkaMetadata.sykmeldingId,
                sykmeldingStatusOpprettet = this.kafkaMetadata.timestamp.toInstant(),
                sykmeldingStatusKafkaMessage =
                    PGobject().also {
                        it.type = "json"
                        it.value = this.serialisertTilString()
                    },
                lokaltOpprettet = now,
            )

        internal fun SykmeldingStatusBufferDbRecord.tilSykmeldingStatusKafkaMessageDTO(): SykmeldingStatusKafkaMessageDTO =
            this.sykmeldingStatusKafkaMessage.value?.let {
                objectMapper.readValue(it)
            }
                ?: throw RuntimeException(
                    "Felt 'sykmeldingStatusKafkaMessage' er null på BuffretSykmeldingHendelseDbRecord, for sykmelding: ${this.sykmeldingId}",
                )
    }
}
