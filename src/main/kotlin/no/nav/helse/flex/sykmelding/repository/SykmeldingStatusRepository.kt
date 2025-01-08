package no.nav.helse.flex.sykmelding.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SykmeldingStatusRepository : CrudRepository<SykmeldingStatusDbRecord, String> {
    fun findBySykmeldingId(sykmeldingId: String): List<SykmeldingStatusDbRecord>
    fun findFirstBySykmeldingIdOrderByTimestampDesc(sykmeldingId: String): SykmeldingStatusDbRecord?
}

data class SykmeldingStatusDbRecord(
    val id: String,
    val sykmeldingUuid: String,
    val timestamp: Instant,
    val status: String,
    val tidligereArbeidsgiver: String?,
    val sporsmal: String?,
    val opprettet: Instant
)
