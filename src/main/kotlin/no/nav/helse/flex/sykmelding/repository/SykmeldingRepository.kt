package no.nav.helse.flex.sykmelding.repository

import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingDbRecord, String> {
    fun findBySykmeldingId(sykmeldingId: String): SykmeldingDbRecord?

    fun findByFnr(fnr: String): List<SykmeldingDbRecord>
}

@Repository
interface SykmeldingStatusRepository : CrudRepository<SykmeldingStatusDbRecord, String> {
    fun findBySykmeldingId(sykmeldingId: String): List<SykmeldingStatusDbRecord>
    fun findFirstBySykmeldingIdOrderByTimestampDesc(sykmeldingId: String): SykmeldingStatusDbRecord?
}

data class SykmeldingDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val fnr: String,
    val behandlingsutfall: String,
    val sykmelding: String,
    val latestStatusId: String?,
    val opprettet: Instant,
    val sendt: Instant? = null,
    val bekreftet: Instant? = null,
    val utgatt: Instant? = null,
    val avbrutt: Instant? = null,
)

data class SykmeldingStatusDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val timestamp: Instant,
    val status: String,
    val arbeidsgiver: String?,
    val sporsmal: String?,
    val opprettet: Instant,
)
