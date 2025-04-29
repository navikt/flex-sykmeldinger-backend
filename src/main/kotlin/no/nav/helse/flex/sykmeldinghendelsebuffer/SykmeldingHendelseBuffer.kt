package no.nav.helse.flex.sykmeldinghendelsebuffer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Component
class SykmeldingHendelseBuffer(
    private val buffretSykmeldingHendelseRepository: BuffretSykmeldingHendelseRepository,
    private val nowFactory: Supplier<Instant>,
) {
    @Transactional(rollbackFor = [Exception::class])
    fun leggTil(hendelse: SykmeldingStatusKafkaMessageDTO) {
        val sykmeldingId = hendelse.kafkaMetadata.sykmeldingId
        val record = hendelse.tilBuffretSykmeldingHendelseDbRecord(now = nowFactory.get())
        aquireBufferLockFor(sykmeldingId)
        buffretSykmeldingHendelseRepository.save(record)
    }

    @Transactional(rollbackFor = [Exception::class])
    fun hentOgFjernAlle(sykmeldingId: String): List<SykmeldingStatusKafkaMessageDTO> {
        aquireBufferLockFor(sykmeldingId)
        val records = buffretSykmeldingHendelseRepository.deleteAllBySykmeldingId(sykmeldingId)
        return records.map { it.tilSykmeldingStatusKafkaMessageDTO() }
    }

    private fun aquireBufferLockFor(sykmeldingId: String) {
        val lockKey = "SykmeldingHendelseBuffer-$sykmeldingId".hashCode()
        buffretSykmeldingHendelseRepository.aquireAdvisoryLock(lockKey)
    }

    companion object {
        internal fun SykmeldingStatusKafkaMessageDTO.tilBuffretSykmeldingHendelseDbRecord(now: Instant): BuffretSykmeldingHendelseDbRecord =
            BuffretSykmeldingHendelseDbRecord(
                sykmeldingId = this.kafkaMetadata.sykmeldingId,
                sykmeldingStatusOpprettet = this.kafkaMetadata.timestamp.toInstant(),
                sykmeldingStatusKafkaMessage =
                    PGobject().apply {
                        type = "json"
                        value = this.serialisertTilString()
                    },
                lokaltOpprettet = now,
            )

        internal fun BuffretSykmeldingHendelseDbRecord.tilSykmeldingStatusKafkaMessageDTO(): SykmeldingStatusKafkaMessageDTO =
            this.sykmeldingStatusKafkaMessage.value?.let {
                objectMapper.readValue(it)
            }
                ?: throw RuntimeException(
                    "Felt 'sykmeldingStatusKafkaMessage' er null på BuffretSykmeldingHendelseDbRecord, for sykmelding: ${this.sykmeldingId}",
                )
    }
}

@Table("buffret_sykmeldinghendelse")
data class BuffretSykmeldingHendelseDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val sykmeldingStatusOpprettet: Instant,
    val sykmeldingStatusKafkaMessage: PGobject,
    val lokaltOpprettet: Instant,
)

interface BuffretSykmeldingHendelseRepository : CrudRepository<BuffretSykmeldingHendelseDbRecord, String> {
    fun findAllBySykmeldingId(sykmeldingId: String): List<BuffretSykmeldingHendelseDbRecord>

    fun deleteAllBySykmeldingId(sykmeldingId: String): List<BuffretSykmeldingHendelseDbRecord>

    @Query("SELECT pg_advisory_xact_lock(:lockKey)")
    fun aquireAdvisoryLock(lockKey: Int)
}
