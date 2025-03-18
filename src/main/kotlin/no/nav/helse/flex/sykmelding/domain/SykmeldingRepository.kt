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
            sykmeldingGrunnlagOppdatert = dbRecord.oppdatert,
            hendelseOppdatert = TODO(),
            validationOppdatert = TODO(),
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
    val oppdatert: Instant,
) {
    fun mapTilSykmelding(): ISykmeldingGrunnlag {
        val serialisertSykmelding = this.sykmelding
        check(serialisertSykmelding.value != null) {
            "sykmelding kolonne burde ikke være null"
        }
        return objectMapper.readValue(serialisertSykmelding.value!!)
    }

    fun mapTilMeldingsinformasjon(): Meldingsinformasjon {
        val serialisertMeldingsinformasjon = this.meldingsinformasjon
        check(serialisertMeldingsinformasjon.value != null) {
            "meldingsinformasjon kolonne burde ikke være null"
        }
        return objectMapper.readValue(serialisertMeldingsinformasjon.value!!)
    }

    fun mapTilValidation(): ValidationResult {
        val serialisertValidation = this.validation
        check(serialisertValidation.value != null) {
            "validation kolonne burde ikke være null"
        }
        return objectMapper.readValue(serialisertValidation.value!!)
    }

    companion object {
        fun mapFraSykmelding(sykmelding: Sykmelding): SykmeldingDbRecord =
            SykmeldingDbRecord(
                id = sykmelding.databaseId,
                sykmeldingId = sykmelding.sykmeldingId,
                fnr = sykmelding.pasientFnr,
                sykmelding =
                    PGobject().apply {
                        type = "json"
                        value = sykmelding.sykmeldingGrunnlag.serialisertTilString()
                    },
                meldingsinformasjon =
                    PGobject().apply {
                        type = "json"
                        value = sykmelding.meldingsinformasjon.serialisertTilString()
                    },
                validation =
                    PGobject().apply {
                        type = "json"
                        value = sykmelding.validation.serialisertTilString()
                    },
                opprettet = sykmelding.opprettet,
                oppdatert = sykmelding.sykmeldingGrunnlagOppdatert,
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
    val sporsmal: PGobject?,
    val arbeidstakerInfo: PGobject?,
    val opprettet: Instant,
) {
    fun mapTilHendelse(): SykmeldingHendelse =
        SykmeldingHendelse(
            databaseId = this.id,
            status = this.status,
            sporsmalSvar =
                this.sporsmal?.value?.let {
                    objectMapper.readValue(it)
                },
            arbeidstakerInfo =
                this.arbeidstakerInfo?.value?.let {
                    objectMapper.readValue(it)
                },
            opprettet = opprettet,
        )

    companion object {
        fun mapFraHendelser(
            statuser: List<SykmeldingHendelse>,
            sykmeldingId: String,
        ): List<SykmeldingHendelseDbRecord> =
            statuser.map { status ->
                SykmeldingHendelseDbRecord(
                    id = status.databaseId,
                    sykmeldingId = sykmeldingId,
                    status = status.status,
                    tidligereArbeidsgiver = null,
                    sporsmal =
                        status.sporsmalSvar?.let { sp ->
                            PGobject().apply {
                                type = "json"
                                value = sp.serialisertTilString()
                            }
                        },
                    arbeidstakerInfo =
                        status.arbeidstakerInfo?.let {
                            PGobject().apply {
                                type = "json"
                                value = it.serialisertTilString()
                            }
                        },
                    opprettet = status.opprettet,
                )
            }
    }
}
