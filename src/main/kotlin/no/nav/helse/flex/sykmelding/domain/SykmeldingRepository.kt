package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.time.Instant

interface ISykmeldingRepository {
    fun save(sykmelding: ISykmelding)

    fun findBySykmeldingUuid(id: String): ISykmelding?

    fun findByFnr(fnr: String): List<ISykmelding>
}

@Repository
class SykmeldingRepository(
    private val sykmeldingDbRepository: SykmeldingDbRepository,
) : ISykmeldingRepository {
    override fun save(sykmelding: ISykmelding) {
        val dbRecord =
            SykmeldingDbRecord(
                sykmeldingUuid = sykmelding.id,
                sisteSykmeldingstatusId = "",
                fnr = sykmelding.pasient.fnr,
                sykmelding =
                    PGobject().apply {
                        type = "json"
                        value = sykmelding.serialisertTilString()
                    },
                opprettet = Instant.now(),
                oppdatert = Instant.now(),
            )
        sykmeldingDbRepository.save(dbRecord)
    }

    override fun findBySykmeldingUuid(id: String): ISykmelding? {
        val dbRecord = sykmeldingDbRepository.findBySykmeldingUuid(id)
        if (dbRecord == null) {
            return null
        }
        return mapDbRecordTilSykmelding(dbRecord)
    }

    override fun findByFnr(fnr: String): List<ISykmelding> {
        val dbRecords = sykmeldingDbRepository.findByFnr(fnr)
        return dbRecords.map(this::mapDbRecordTilSykmelding)
    }

    private fun mapDbRecordTilSykmelding(dbRecord: SykmeldingDbRecord): ISykmelding {
        val serialisertSykmelding = dbRecord.sykmelding
        check(serialisertSykmelding.value != null) {
            "sykmelding kolonne burde ikke være null"
        }
        return objectMapper.readValue(serialisertSykmelding.value!!)
    }
}

@Repository
interface SykmeldingDbRepository : CrudRepository<SykmeldingDbRecord, String> {
    fun findByFnr(fnr: String): List<SykmeldingDbRecord>

    fun findBySykmeldingUuid(sykmeldingUuid: String): SykmeldingDbRecord?
}

data class SykmeldingDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingUuid: String,
    val sisteSykmeldingstatusId: String,
    val fnr: String,
    val sykmelding: PGobject,
    val opprettet: Instant,
    val oppdatert: Instant?,
)
