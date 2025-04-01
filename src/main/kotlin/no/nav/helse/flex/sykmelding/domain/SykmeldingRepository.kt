package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.domain.tsm.ISykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.tsm.Meldingsinformasjon
import no.nav.helse.flex.sykmelding.domain.tsm.ValidationResult
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
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

    fun findAllByPersonIdenter(identer: PersonIdenter): List<Sykmelding>

    fun findAll(): List<Sykmelding>

    fun deleteAll()
}

@Repository
class SykmeldingRepository(
    private val sykmeldingDbRepository: SykmeldingDbRepository,
    private val sykmeldingHendelseDbRepository: SykmeldingHendelseDbRepository,
) : ISykmeldingRepository {
    @Transactional
    override fun save(sykmelding: Sykmelding): Sykmelding {
        val hendelser = sykmelding.hendelser

        val statusDbRecords = SykmeldingHendelseDbRecord.mapFraHendelser(hendelser, sykmelding.sykmeldingId)
        val sykmeldingDbRecord = SykmeldingDbRecord.mapFraSykmelding(sykmelding)

        val lagretSykmeldingDbRecord = sykmeldingDbRepository.save(sykmeldingDbRecord)
        val lagredeStatusDbRecords = sykmeldingHendelseDbRepository.saveAll(statusDbRecords)
        return mapTilSykmelding(lagretSykmeldingDbRecord, lagredeStatusDbRecords)
    }

    override fun findBySykmeldingId(id: String): Sykmelding? {
        val dbRecord = sykmeldingDbRepository.findBySykmeldingId(id)
        if (dbRecord == null) {
            return null
        }
        val statusDbRecords = sykmeldingHendelseDbRepository.findAllBySykmeldingId(dbRecord.sykmeldingId)
        return mapTilSykmelding(dbRecord, statusDbRecords)
    }

    override fun findAllByPersonIdenter(identer: PersonIdenter): List<Sykmelding> {
        val dbRecords = sykmeldingDbRepository.findAllByFnrIn(identer.alle())
        val statusDbRecords =
            sykmeldingHendelseDbRepository.findAllBySykmeldingIdIn(dbRecords.map { it.sykmeldingId })
        return dbRecords.map { dbRecord ->
            val statusDbRecords = statusDbRecords.filter { it.sykmeldingId == dbRecord.sykmeldingId }
            mapTilSykmelding(dbRecord, statusDbRecords)
        }
    }

    override fun findAll(): List<Sykmelding> {
        val dbRecords = sykmeldingDbRepository.findAll()
        val statusDbRecords = sykmeldingHendelseDbRepository.findAll()
        return dbRecords.map { dbRecord ->
            val statusDbRecords = statusDbRecords.filter { it.sykmeldingId == dbRecord.sykmeldingId }
            mapTilSykmelding(dbRecord, statusDbRecords)
        }
    }

    override fun deleteAll() {
        sykmeldingHendelseDbRepository.deleteAll()
        sykmeldingDbRepository.deleteAll()
    }

    private fun mapTilSykmelding(
        dbRecord: SykmeldingDbRecord,
        statusDbRecords: Iterable<SykmeldingHendelseDbRecord>,
    ): Sykmelding =
        Sykmelding(
            databaseId = dbRecord.id,
            sykmeldingGrunnlag = dbRecord.mapTilSykmelding(),
            meldingsinformasjon = dbRecord.mapTilMeldingsinformasjon(),
            validation = dbRecord.mapTilValidation(),
            hendelser = statusDbRecords.map(SykmeldingHendelseDbRecord::mapTilHendelse),
            opprettet = dbRecord.opprettet,
            sykmeldingGrunnlagOppdatert = dbRecord.sykmeldingGrunnlagOppdatert,
            hendelseOppdatert = dbRecord.hendelseOppdatert,
            validationOppdatert = dbRecord.validationOppdatert,
        )
}

@Repository
interface SykmeldingDbRepository : CrudRepository<SykmeldingDbRecord, String> {
    fun findAllByFnrIn(identer: List<String>): List<SykmeldingDbRecord>

    fun findBySykmeldingId(sykmeldingUuid: String): SykmeldingDbRecord?
}

@Table("sykmelding")
data class SykmeldingDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val fnr: String,
    val sykmelding: PGobject,
    val meldingsinformasjon: PGobject,
    val validation: PGobject,
    val opprettet: Instant,
    val hendelseOppdatert: Instant,
    val sykmeldingGrunnlagOppdatert: Instant,
    val validationOppdatert: Instant,
) {
    fun mapTilSykmelding(): ISykmeldingGrunnlag =
        this.sykmelding.fraPsqlJson()
            ?: error("sykmelding kolonne burde ikke være null")

    fun mapTilMeldingsinformasjon(): Meldingsinformasjon =
        this.meldingsinformasjon.fraPsqlJson()
            ?: error("meldingsinformasjon kolonne burde ikke være null")

    fun mapTilValidation(): ValidationResult =
        this.validation.fraPsqlJson()
            ?: error("validation kolonne burde ikke være null")

    companion object {
        fun mapFraSykmelding(sykmelding: Sykmelding): SykmeldingDbRecord =
            SykmeldingDbRecord(
                id = sykmelding.databaseId,
                sykmeldingId = sykmelding.sykmeldingId,
                fnr = sykmelding.pasientFnr,
                sykmelding = sykmelding.sykmeldingGrunnlag.tilPsqlJson(),
                meldingsinformasjon = sykmelding.meldingsinformasjon.tilPsqlJson(),
                validation = sykmelding.validation.tilPsqlJson(),
                opprettet = sykmelding.opprettet,
                hendelseOppdatert = sykmelding.hendelseOppdatert,
                sykmeldingGrunnlagOppdatert = sykmelding.sykmeldingGrunnlagOppdatert,
                validationOppdatert = sykmelding.validationOppdatert,
            )
    }
}

interface SykmeldingHendelseDbRepository : CrudRepository<SykmeldingHendelseDbRecord, String> {
    fun findAllBySykmeldingId(sykmeldingUuid: String): List<SykmeldingHendelseDbRecord>

    fun findAllBySykmeldingIdIn(sykmeldingUuid: Collection<String>): List<SykmeldingHendelseDbRecord>
}

@Table("sykmeldinghendelse")
data class SykmeldingHendelseDbRecord(
    @Id
    val id: String? = null,
    val sykmeldingId: String,
    val status: HendelseStatus,
    val tidligereArbeidsgiver: PGobject?,
    val arbeidstakerInfo: PGobject?,
    val tilleggsinfo: PGobject?,
    val brukerSvar: PGobject?,
    val opprettet: Instant,
) {
    fun mapTilHendelse(): SykmeldingHendelse =
        SykmeldingHendelse(
            databaseId = this.id,
            status = this.status,
            arbeidstakerInfo = this.arbeidstakerInfo?.fraPsqlJson(),
            tilleggsinfo = this.tilleggsinfo?.fraPsqlJson(),
            brukerSvar = this.brukerSvar?.fraPsqlJson(),
            opprettet = opprettet,
        )

    companion object {
        fun mapFraHendelser(
            hendelser: List<SykmeldingHendelse>,
            sykmeldingId: String,
        ): List<SykmeldingHendelseDbRecord> = hendelser.map { mapFraHendelse(it, sykmeldingId) }

        private fun mapFraHendelse(
            hendelse: SykmeldingHendelse,
            sykmeldingId: String,
        ): SykmeldingHendelseDbRecord =
            SykmeldingHendelseDbRecord(
                id = hendelse.databaseId,
                sykmeldingId = sykmeldingId,
                status = hendelse.status,
                tidligereArbeidsgiver = null,
                arbeidstakerInfo = hendelse.arbeidstakerInfo?.tilPsqlJson(),
                tilleggsinfo = hendelse.tilleggsinfo?.tilPsqlJson(),
                brukerSvar = hendelse.brukerSvar?.tilPsqlJson(),
                opprettet = hendelse.opprettet,
            )
    }
}

private fun Any.tilPsqlJson(): PGobject {
    val pgObject = PGobject()
    pgObject.type = "json"
    pgObject.value = this.serialisertTilString()
    return pgObject
}

private inline fun <reified T> PGobject.fraPsqlJson(): T? =
    this.value?.let {
        objectMapper.readValue<T>(it)
    }
