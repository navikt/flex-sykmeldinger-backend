package no.nav.helse.flex.sykmeldinghendelsebuffer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.config.AdvisoryLock
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.postgresql.util.PGobject
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingHendelseBuffer(
    private val sykmeldingHendelseBufferRepository: SykmeldingHendelseBufferRepository,
    private val advisoryLock: AdvisoryLock,
    private val nowFactory: Supplier<Instant>,
) {
    @Transactional(rollbackFor = [Exception::class])
    fun leggTil(hendelse: SykmeldingStatusKafkaMessageDTO) {
        val sykmeldingId = hendelse.kafkaMetadata.sykmeldingId
        val record = hendelse.tilBuffretSykmeldingHendelseDbRecord(now = nowFactory.get())
        aquireBufferLockFor(sykmeldingId)
        sykmeldingHendelseBufferRepository.save(record)
    }

    fun kikkPaaAlleFor(sykmeldingId: String): List<SykmeldingStatusKafkaMessageDTO> =
        sykmeldingHendelseBufferRepository
            .findAllBySykmeldingId(sykmeldingId)
            .map { it.tilSykmeldingStatusKafkaMessageDTO() }

    @Transactional(rollbackFor = [Exception::class])
    fun prosesserAlleFor(sykmeldingId: String): List<SykmeldingStatusKafkaMessageDTO> {
        aquireBufferLockFor(sykmeldingId)
        val records = sykmeldingHendelseBufferRepository.findAllBySykmeldingId(sykmeldingId)
        sykmeldingHendelseBufferRepository.deleteAll(records)
        return records
            .sortedBy { it.sykmeldingStatusOpprettet }
            .map { it.tilSykmeldingStatusKafkaMessageDTO() }
    }

    private fun aquireBufferLockFor(sykmeldingId: String) {
        advisoryLock.acquire("SykmeldingHendelseBuffer", sykmeldingId)
    }

    companion object {
        internal fun SykmeldingStatusKafkaMessageDTO.tilBuffretSykmeldingHendelseDbRecord(now: Instant): SykmeldingHendelseBufferDbRecord =
            SykmeldingHendelseBufferDbRecord(
                sykmeldingId = this.kafkaMetadata.sykmeldingId,
                sykmeldingStatusOpprettet = this.kafkaMetadata.timestamp.toInstant(),
                sykmeldingStatusKafkaMessage =
                    PGobject().also {
                        it.type = "json"
                        it.value = this.serialisertTilString()
                    },
                lokaltOpprettet = now,
            )

        internal fun SykmeldingHendelseBufferDbRecord.tilSykmeldingStatusKafkaMessageDTO(): SykmeldingStatusKafkaMessageDTO =
            this.sykmeldingStatusKafkaMessage.value?.let {
                objectMapper.readValue(it)
            }
                ?: throw RuntimeException(
                    "Felt 'sykmeldingStatusKafkaMessage' er null på BuffretSykmeldingHendelseDbRecord, for sykmelding: ${this.sykmeldingId}",
                )
    }
}
