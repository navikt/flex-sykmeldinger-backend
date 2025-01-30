package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.postgresql.util.PGobject
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface ISykmeldingRepository {
    fun save(sykmelding: Sykmelding): Sykmelding

    fun findBySykmeldingId(id: String): Sykmelding?

    fun findAllByFnr(fnr: String): List<Sykmelding>

    fun findAll(): List<Sykmelding>

    fun deleteAll()
}

@Repository
class SykmeldingRepository(
    private val sykmeldingDbRepository: SykmeldingDbRepository,
    private val sykmeldingStatusDbRepository: SykmeldingStatusDbRepository,
) : ISykmeldingRepository {
    @Transactional
    override fun save(sykmelding: Sykmelding): Sykmelding {
        val sykmeldingGrunnlag = sykmelding.sykmeldingGrunnlag
        val statuser = sykmelding.statuser

        val statusDbRecords = SykmeldingStatusDbRecord.mapFraStatus(statuser, sykmelding.sykmeldingId)
        val sykmeldingDbRecord = SykmeldingDbRecord.mapFraSykmelding(sykmelding, sykmeldingGrunnlag)

        val lagretSykmeldingDbRecord = sykmeldingDbRepository.save(sykmeldingDbRecord)
        val lagredeStatusDbRecords = sykmeldingStatusDbRepository.saveAll(statusDbRecords)
        return mapTilSykmelding(lagretSykmeldingDbRecord, lagredeStatusDbRecords)
    }

    override fun findBySykmeldingId(id: String): Sykmelding? {
        val dbRecord = sykmeldingDbRepository.findBySykmeldingUuid(id)
        if (dbRecord == null) {
            return null
        }
        val statusDbRecords = sykmeldingStatusDbRepository.findAllBySykmeldingUuid(dbRecord.sykmeldingUuid)
        return mapTilSykmelding(dbRecord, statusDbRecords)
    }

    override fun findAllByFnr(fnr: String): List<Sykmelding> {
        val dbRecords = sykmeldingDbRepository.findByFnr(fnr)
        val statusDbRecords =
            sykmeldingStatusDbRepository.findAllBySykmeldingUuidIn(dbRecords.map { it.sykmeldingUuid })
        return dbRecords.map { dbRecord ->
            val statusDbRecords = statusDbRecords.filter { it.sykmeldingUuid == dbRecord.sykmeldingUuid }
            mapTilSykmelding(dbRecord, statusDbRecords)
        }
    }

    override fun findAll(): List<Sykmelding> {
        val dbRecords = sykmeldingDbRepository.findAll()
        val statusDbRecords = sykmeldingStatusDbRepository.findAll()
        return dbRecords.map { dbRecord ->
            val statusDbRecords = statusDbRecords.filter { it.sykmeldingUuid == dbRecord.sykmeldingUuid }
            mapTilSykmelding(dbRecord, statusDbRecords)
        }
    }

    override fun deleteAll() {
        sykmeldingStatusDbRepository.deleteAll()
        sykmeldingDbRepository.deleteAll()
    }

    private fun mapTilSykmelding(
        dbRecord: SykmeldingDbRecord,
        statusDbRecords: Iterable<SykmeldingStatusDbRecord>,
    ): Sykmelding =
        Sykmelding(
            databaseId = dbRecord.id,
            sykmeldingGrunnlag = dbRecord.mapTilSykmelding(),
            statuser = statusDbRecords.map(SykmeldingStatusDbRecord::mapTilStatus),
            opprettet = dbRecord.opprettet,
            oppdatert = dbRecord.oppdatert,
        )
}

@Repository
interface SykmeldingDbRepository : CrudRepository<SykmeldingDbRecord, String> {
    fun findByFnr(fnr: String): List<SykmeldingDbRecord>

    fun findBySykmeldingUuid(sykmeldingUuid: String): SykmeldingDbRecord?
}

@Table("sykmelding")
data class SykmeldingDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingUuid: String,
    val fnr: String,
    val sykmelding: PGobject,
    val opprettet: Instant,
    val oppdatert: Instant,
) {
    fun mapTilSykmelding(): ISykmeldingGrunnlag {
        val serialisertSykmelding = this.sykmelding
        check(serialisertSykmelding.value != null) {
            "sykmelding kolonne burde ikke være null"
        }
        return objectMapper.readValue(serialisertSykmelding.value!!)
    }

    companion object {
        fun mapFraSykmelding(
            sykmelding: Sykmelding,
            sykmeldingGrunnlag: ISykmeldingGrunnlag,
        ): SykmeldingDbRecord =
            SykmeldingDbRecord(
                id = sykmelding.databaseId,
                sykmeldingUuid = sykmelding.sykmeldingId,
                fnr = sykmeldingGrunnlag.pasient.fnr,
                sykmelding =
                    PGobject().apply {
                        type = "json"
                        value = sykmeldingGrunnlag.serialisertTilString()
                    },
                opprettet = sykmelding.opprettet,
                oppdatert = sykmelding.oppdatert,
            )
    }
}

interface SykmeldingStatusDbRepository : CrudRepository<SykmeldingStatusDbRecord, String> {
    fun findAllBySykmeldingUuid(sykmeldingUuid: String): List<SykmeldingStatusDbRecord>

    fun findAllBySykmeldingUuidIn(sykmeldingUuid: Collection<String>): List<SykmeldingStatusDbRecord>
}

@Table("sykmeldingstatus")
data class SykmeldingStatusDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingUuid: String,
    val status: StatusEvent,
    val tidligereArbeidsgiver: PGobject?,
    val sporsmal: PGobject?,
    val opprettet: Instant,
) {
    fun mapTilStatus(): SykmeldingStatus =
        SykmeldingStatus(
            databaseId = this.id,
            status = this.status,
            sporsmalSvar =
                this.sporsmal?.value?.let {
                    objectMapper.readValue(it)
                },
            opprettet = opprettet,
        )

    companion object {
        fun mapFraStatus(
            statuser: List<SykmeldingStatus>,
            sykmeldingId: String,
        ): List<SykmeldingStatusDbRecord> =
            statuser.map { status ->
                SykmeldingStatusDbRecord(
                    id = status.databaseId,
                    sykmeldingUuid = sykmeldingId,
                    status = status.status,
                    tidligereArbeidsgiver = null,
                    sporsmal =
                        status.sporsmalSvar?.let { sp ->
                            PGobject().apply {
                                type = "json"
                                value = sp.serialisertTilString()
                            }
                        },
                    opprettet = status.opprettet,
                )
            }
    }
}
