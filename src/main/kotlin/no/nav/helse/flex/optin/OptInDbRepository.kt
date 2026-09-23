package no.nav.helse.flex.optin

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OptInDbRepository : CrudRepository<OptInDbRecord, Long> {
    fun findAllBySykmeldingId(sykmeldingId: String): List<OptInDbRecord>
}
