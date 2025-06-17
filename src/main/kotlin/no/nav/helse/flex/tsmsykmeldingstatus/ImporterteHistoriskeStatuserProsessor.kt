package no.nav.helse.flex.tsmsykmeldingstatus

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.config.AdvisoryLock
import no.nav.helse.flex.sykmelding.domain.SykmeldingRepository
import no.nav.helse.flex.tsmsykmeldingstatus.dto.StatusEventKafkaDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.function.Supplier

@Service
class ImporterteHistoriskeStatuserProsessor(
    private val tsmHistoriskeSykmeldingstatusDao: TsmHistoriskeSykmeldingstatusDao,
    private val advisoryLock: AdvisoryLock,
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingHendelseFraKafkaKonverterer: SykmeldingHendelseFraKafkaKonverterer,
    private val nowFactory: Supplier<Instant>,
) {
    private val log = logger()

    enum class ResultatStatus {
        OK,
        PROV_IGJEN,
        FERDIG,
    }

    data class Resultat(
        val status: ResultatStatus,
        val antallProsessert: Int = 0,
        val antallLagtTil: Int = 0,
    )

    companion object {
        const val SERVICE_LOCK_KEY = "ImporterteHistoriskeStatuserProsessor"
    }

    @Transactional(rollbackFor = [Exception::class])
    fun prosesserNesteSykmeldingStatuser(): Resultat {
        val sykmeldingIder: List<String> =
            tsmHistoriskeSykmeldingstatusDao.lesNesteSykmeldingIderForBehandling()
        if (sykmeldingIder.isEmpty()) {
            return Resultat(status = ResultatStatus.FERDIG)
        }

        val sykmeldingId =
            sykmeldingIder.firstOrNull { id ->
                advisoryLock.tryAcquire(SERVICE_LOCK_KEY, id)
            }
        if (sykmeldingId == null) {
            return Resultat(status = ResultatStatus.PROV_IGJEN)
        }

        val statuser = tsmHistoriskeSykmeldingstatusDao.lesAlleStatuserForSykmelding(sykmeldingId)
        var statuserLagtTil = 0
        for (status in statuser) {
            val lagtTil = leggTilStatus(status)
            if (lagtTil) {
                statuserLagtTil++
            }
        }
        tsmHistoriskeSykmeldingstatusDao.settAlleStatuserForSykmeldingLest(
            sykmeldingId,
            tidspunkt = nowFactory.get(),
        )
        return Resultat(status = ResultatStatus.OK, antallLagtTil = statuserLagtTil, antallProsessert = statuser.size)
    }

    private fun leggTilStatus(status: SykmeldingStatusKafkaDTO): Boolean {
        val sykmeldingId = status.sykmeldingId
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            log.warn(
                "Fant ikke sykmelding for importert status, sykmelding: $sykmeldingId, status: ${status.statusEvent}. Hopper over status",
            )
            return false
        } else {
            when (val statusEvent = status.statusEvent) {
                StatusEventKafkaDTO.SLETTET,
                StatusEventKafkaDTO.APEN,
                -> {
                    log.debug("Ignorerer importert status $statusEvent for sykmelding '$sykmeldingId'")
                    return false
                }
                else -> {
                    val hendelse =
                        sykmeldingHendelseFraKafkaKonverterer.konverterSykmeldingHendelseFraKafkaDTO(
                            status = status,
                            erSykmeldingAvvist = sykmelding.erAvvist,
                            source = "importert-historisk-status",
                        )
                    if (SykmeldingStatusHandterer.finnesDuplikatHendelsePaaSykmelding(
                            sykmelding = sykmelding,
                            sykmeldingHendelse = hendelse,
                        )
                    ) {
                        log.info(
                            "Importert sykmelding hendelse eksisterer allerede, sykmelding: $sykmeldingId, status: ${hendelse.status}. " +
                                "Lagrer ikke hendelse",
                        )
                        return false
                    }
                    val oppdatertSykmelding = sykmelding.leggTilHendelse(hendelse)
                    sykmeldingRepository.save(oppdatertSykmelding)
                    log.info("Importert sykmelding hendelse lagret, sykmelding: $sykmeldingId, status: ${hendelse.status}")
                    return true
                }
            }
        }
    }
}

@Component
class TsmHistoriskeSykmeldingstatusDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    companion object {
        private val sisteDato: LocalDate = LocalDate.parse("2020-05-01")
    }

    fun lesNesteSykmeldingIderForBehandling(): List<String> =
        jdbcTemplate.queryForList(
            """
            WITH
            status as (
                SELECT * FROM temp_tsm_historisk_sykmeldingstatus
            ),
            innlesing AS (
                SELECT * FROM temp_tsm_historisk_sykmeldingstatus_innlesing
            ),
            relevante_sykmelding_id AS (
                SELECT
                    sykmelding_id,
                    min(status.timestamp) as timestamp
                FROM status
                LEFT JOIN innlesing
                    USING (sykmelding_id)
                WHERE 
                    innlesing.sykmelding_id IS NULL
                    AND status.timestamp <= :sisteDato
                GROUP BY sykmelding_id
            )
            SELECT sykmelding_id
            FROM relevante_sykmelding_id
            ORDER BY timestamp ASC
            LIMIT 100
            """.trimIndent(),
            mapOf(
                "sisteDato" to sisteDato,
            ),
            String::class.java,
        )

    fun lesAlleBehandledeSykmeldingIder(): List<String> =
        jdbcTemplate.queryForList(
            """
            SELECT sykmelding_id
            FROM temp_tsm_historisk_sykmeldingstatus_innlesing
            """.trimIndent(),
            emptyMap<String, Any>(),
            String::class.java,
        )

    fun lesAlleStatuserForSykmelding(sykmeldingId: String): List<SykmeldingStatusKafkaDTO> =
        jdbcTemplate.query(
            """
            SELECT *
            FROM temp_tsm_historisk_sykmeldingstatus
            WHERE sykmelding_id = :sykmeldingId
            ORDER BY timestamp ASC
            """.trimIndent(),
            mapOf("sykmeldingId" to sykmeldingId),
            TsmSykmeldingerRowMapper,
        )

    fun settAlleStatuserForSykmeldingLest(
        sykmeldingId: String,
        tidspunkt: Instant = Instant.now(),
    ) {
        val inserter: SimpleJdbcInsert =
            SimpleJdbcInsert(jdbcTemplate.jdbcTemplate)
                .withTableName("temp_tsm_historisk_sykmeldingstatus_innlesing")

        inserter.execute(
            mapOf(
                "sykmelding_id" to sykmeldingId,
                "lest_tidspunkt" to Timestamp.from(tidspunkt),
            ),
        )
    }
}

internal object TsmSykmeldingerRowMapper : RowMapper<SykmeldingStatusKafkaDTO> {
    override fun mapRow(
        rs: ResultSet,
        rowNum: Int,
    ): SykmeldingStatusKafkaDTO =
        SykmeldingStatusKafkaDTO(
            sykmeldingId = rs.getString("sykmelding_id"),
            timestamp = rs.getObject("timestamp", OffsetDateTime::class.java),
            statusEvent = rs.getString("event"),
            arbeidsgiver =
                rs.getObject("arbeidsgiver", PGobject::class.java)?.value?.let {
                    objectMapper.readValue(it)
                },
            tidligereArbeidsgiver =
                rs.getObject("tidligere_arbeidsgiver", PGobject::class.java)?.value?.let {
                    objectMapper.readValue(it)
                },
            brukerSvar =
                rs.getObject("alle_sprosmal", PGobject::class.java)?.value?.let {
                    objectMapper.readValue(it)
                },
            sporsmals =
                rs.getObject("sporsmal", PGobject::class.java)?.value?.let {
                    objectMapper.readValue(it)
                },
        )
}
