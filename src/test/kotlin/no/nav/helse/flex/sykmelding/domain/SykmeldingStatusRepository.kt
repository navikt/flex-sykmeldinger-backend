package no.nav.helse.flex.sykmelding.domain

import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface SykmeldingStatusRepository : CrudRepository<SykmeldingStatusDbRecord, String> {
    fun findBySykmeldingUuid(sykmeldingUuid: String): List<SykmeldingStatusDbRecord>

    fun findFirstBySykmeldingUuidOrderByTimestampDesc(sykmeldingUuid: String): SykmeldingStatusDbRecord?
}

data class SykmeldingStatusDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingUuid: String,
    val timestamp: Instant,
    val status: String,
    val tidligereArbeidsgiver: String?,
    val sporsmal: String?,
    val opprettet: Instant,
)
