package no.nav.helse.flex.sykmelding.domain

import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SykmeldingRepository : CrudRepository<SykmeldingDbRecord, String> {
    fun findByFnr(fnr: String): List<SykmeldingDbRecord>

    fun findBySykmeldingUuid(sykmeldingUuid: String): SykmeldingDbRecord?
}

data class SykmeldingDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingUuid: String,
    val sisteSykmeldingstatusId: String,
    val fnr: String,
    val sykmelding: String,
    val person: String,
    val opprettet: Instant,
    val oppdatert: Instant?,
)
