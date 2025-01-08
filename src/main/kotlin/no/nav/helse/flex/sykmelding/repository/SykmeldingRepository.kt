package no.nav.helse.flex.sykmelding.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingDbRecord, String> {
    fun findByFnr(fnr: String): List<SykmeldingDbRecord>
    fun findBySykmeldingId(sykmeldingId: String): SykmeldingDbRecord?
}

data class SykmeldingDbRecord(
    val id: String,
    val sykmeldingUuid: String,
    val sisteSykmeldingstatusId: String,
    val fnr: String,
    val sykmelding: String,
    val person: String,
    val opprettet: Instant,
    val oppdatert: Instant?
)
