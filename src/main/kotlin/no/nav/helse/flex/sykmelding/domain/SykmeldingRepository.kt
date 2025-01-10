package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface ISykmeldingRepository {
    fun save(sykmelding: Sykmelding)

    fun findBySykmeldingUuid(id: String): Sykmelding?

    fun findByFnr(fnr: String): List<Sykmelding>
}

@Repository
class SykmeldingRepository(
    private val sykmeldingDbRepository: SykmeldingDbRepository,
    private val sykmeldingStatusRepository: SykmeldingStatusRepository,
) : ISykmeldingRepository {
    @Transactional
    override fun save(sykmelding: Sykmelding) {
        val sykmeldingGrunnlag = sykmelding.sykmeldingGrunnlag
        val statuser = sykmelding.statuser

        val statusDbRecords =
            statuser.map { status ->
                val timestamp = Instant.now()
                SykmeldingStatusDbRecord(
                    sykmeldingUuid = sykmelding.sykmeldingId,
                    status = status.status,
                    timestamp = timestamp,
                    tidligereArbeidsgiver = null,
                    sporsmal = null,
                    opprettet = timestamp,
                )
            }

        val dbRecord =
            SykmeldingDbRecord(
                sykmeldingUuid = sykmelding.sykmeldingId,
                sisteSykmeldingstatusId = "",
                fnr = sykmeldingGrunnlag.pasient.fnr,
                sykmelding =
                PGobject().apply {
                    type = "json"
                    value = sykmelding.serialisertTilString()
                },
                opprettet = Instant.now(),
                oppdatert = Instant.now(),
            )

        sykmeldingDbRepository.save(dbRecord)
        sykmeldingStatusRepository.saveAll(statusDbRecords)
    }

    override fun findBySykmeldingUuid(id: String): Sykmelding? {
        val dbRecord = sykmeldingDbRepository.findBySykmeldingUuid(id)
        if (dbRecord == null) {
            return null
        }
        val statusDbRecords = sykmeldingStatusRepository.findAllBySykmeldingUuid(dbRecord.sykmeldingUuid)
        return mapTilFlexSykmelding(dbRecord, statusDbRecords)
    }

    override fun findByFnr(fnr: String): List<Sykmelding> {
        val dbRecords = sykmeldingDbRepository.findByFnr(fnr)
        val statusDbRecords = sykmeldingStatusRepository.findAllBySykmeldingUuidIn(dbRecords.map { it.sykmeldingUuid })
        return dbRecords.map { dbRecord ->
            val statusDbRecords = statusDbRecords.filter { it.sykmeldingUuid == dbRecord.sykmeldingUuid }
            mapTilFlexSykmelding(dbRecord, statusDbRecords)
        }
    }

    private fun mapTilFlexSykmelding(
        dbRecord: SykmeldingDbRecord,
        statusDbRecords: List<SykmeldingStatusDbRecord>,
    ): Sykmelding {
        return Sykmelding(
            sykmeldingGrunnlag = mapDbRecordTilSykmelding(dbRecord),
            statuser = statusDbRecords.map(this::mapStatusDbRecordTilStatus),
        )
    }

    private fun mapDbRecordTilSykmelding(dbRecord: SykmeldingDbRecord): ISykmeldingGrunnlag {
        val serialisertSykmelding = dbRecord.sykmelding
        check(serialisertSykmelding.value != null) {
            "sykmelding kolonne burde ikke være null"
        }
        return objectMapper.readValue(serialisertSykmelding.value!!)
    }

    private fun mapStatusDbRecordTilStatus(statusDbRecord: SykmeldingStatusDbRecord): SykmeldingStatus {
        return SykmeldingStatus(
            status = statusDbRecord.status,
            sporsmal = statusDbRecord.sporsmal,
            timestamp = statusDbRecord.timestamp,
        )
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

interface SykmeldingStatusRepository : CrudRepository<SykmeldingStatusDbRecord, String> {
    fun findAllBySykmeldingUuid(sykmeldingUuid: String): List<SykmeldingStatusDbRecord>

    fun findAllBySykmeldingUuidIn(sykmeldingUuid: Collection<String>): List<SykmeldingStatusDbRecord>
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
