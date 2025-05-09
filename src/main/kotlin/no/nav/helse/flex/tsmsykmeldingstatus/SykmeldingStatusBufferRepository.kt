package no.nav.helse.flex.tsmsykmeldingstatus

import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import java.time.Instant

@Table("sykmeldinghendelse_buffer")
data class SykmeldingStatusBufferDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val sykmeldingStatusOpprettet: Instant,
    val sykmeldingStatusKafkaMessage: PGobject,
    val lokaltOpprettet: Instant,
)

interface SykmeldingStatusBufferRepository : CrudRepository<SykmeldingStatusBufferDbRecord, String> {
    fun findAllBySykmeldingId(sykmeldingId: String): List<SykmeldingStatusBufferDbRecord>
}
