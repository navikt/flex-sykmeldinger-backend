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
import java.util.function.Supplier

interface ISykmeldingRepository {
    fun save(sykmelding: Sykmelding)

    fun findBySykmeldingId(id: String): Sykmelding?

    fun findAllByFnr(fnr: String): List<Sykmelding>

    fun findAll(): List<Sykmelding>

    fun deleteAll()
}

@Repository
class SykmeldingRepository(
    private val sykmeldingDbRepository: SykmeldingDbRepository,
    private val sykmeldingStatusDbRepository: SykmeldingStatusDbRepository,
    private val nowFactory: Supplier<Instant>,
) : ISykmeldingRepository {

    @Transactional
    override fun save(sykmelding: Sykmelding) {
        val sykmeldingGrunnlag = sykmelding.sykmeldingGrunnlag
        val statuser = sykmelding.statuser

        val statusDbRecords = SykmeldingStatusDbRecord.mapFraStatus(statuser, sykmelding.sykmeldingId, nowFactory)
        val sykmeldingDbRecord = SykmeldingDbRecord.mapFraSykmelding(sykmelding, sykmeldingGrunnlag, nowFactory)

        sykmeldingDbRepository.save(sykmeldingDbRecord)
        sykmeldingStatusDbRepository.saveAll(statusDbRecords)
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
        statusDbRecords: List<SykmeldingStatusDbRecord>,
    ): Sykmelding {
        return Sykmelding(
            databaseId = dbRecord.id,
            sykmeldingGrunnlag = dbRecord.mapTilSykmelding(),
            statuser = statusDbRecords.map(SykmeldingStatusDbRecord::mapTilStatus),
        )
    }
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
    val oppdatert: Instant?,
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
            nowFactory: Supplier<Instant> = Supplier { Instant.now() },
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
                // TODO: Ikke sett ved oppdatering
                opprettet = nowFactory.get(),
                oppdatert = nowFactory.get(),
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
    val timestamp: Instant,
    val status: String,
    val tidligereArbeidsgiver: PGobject?,
    val sporsmal: PGobject?,
    val opprettet: Instant,
) {
    fun mapTilStatus(): SykmeldingStatus {
        return SykmeldingStatus(
            databaseId = this.id,
            status = this.status,
            sporsmalSvar =
            this.sporsmal?.value?.let {
                objectMapper.readValue(it)
            },
            timestamp = this.timestamp,
        )
    }

    companion object {
        fun mapFraStatus(
            statuser: List<SykmeldingStatus>,
            sykmeldingId: String,
            nowFactory: Supplier<Instant> = Supplier { Instant.now() },
        ): List<SykmeldingStatusDbRecord> =
            statuser.map { status ->
                SykmeldingStatusDbRecord(
                    id = status.databaseId,
                    sykmeldingUuid = sykmeldingId,
                    status = status.status,
                    timestamp = status.timestamp,
                    tidligereArbeidsgiver = null,
                    sporsmal =
                    status.sporsmalSvar?.let { sp ->
                        PGobject().apply {
                            type = "json"
                            value = sp.serialisertTilString()
                        }
                    },
                    // TODO: Ikke sett ved oppdatering
                    opprettet = nowFactory.get(),
                )
            }
    }
}
