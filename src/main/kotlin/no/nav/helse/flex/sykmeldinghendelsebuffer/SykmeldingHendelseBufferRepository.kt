package no.nav.helse.flex.sykmeldinghendelsebuffer

import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.Instant

@Table("sykmeldinghendelse_buffer")
data class SykmeldingHendelseBufferDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val sykmeldingStatusOpprettet: Instant,
    val sykmeldingStatusKafkaMessage: PGobject,
    val lokaltOpprettet: Instant,
)

interface SykmeldingHendelseBufferRepository : CrudRepository<SykmeldingHendelseBufferDbRecord, String> {
    fun findAllBySykmeldingId(sykmeldingId: String): List<SykmeldingHendelseBufferDbRecord>

    fun deleteAllBySykmeldingId(sykmeldingId: String): List<SykmeldingHendelseBufferDbRecord>
}
