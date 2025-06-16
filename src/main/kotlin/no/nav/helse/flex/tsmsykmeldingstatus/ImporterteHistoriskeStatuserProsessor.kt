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
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDate
import java.time.OffsetDateTime

@Component
class ImporterteHistoriskeStatuserProsessor(
    private val tsmHistoriskeSykmeldingstatusDao: TsmHistoriskeSykmeldingstatusDao,
    private val advisoryLock: AdvisoryLock,
    private val sykmeldingRepository: SykmeldingRepository,
    private val sykmeldingHendelseFraKafkaKonverterer: SykmeldingHendelseFraKafkaKonverterer,
) {
    private val log = logger()

    enum class Resultat {
        OK,
        PROV_IGJEN,
        FERDIG,
    }

    @Transactional(rollbackFor = [Exception::class])
    fun prosesserNesteSykmeldingStatuser(): Resultat {
        val sykmeldingIder: List<String> =
            tsmHistoriskeSykmeldingstatusDao.lesNesteSykmeldingIderForBehandling()
        if (sykmeldingIder.isEmpty()) {
            return Resultat.FERDIG
        }

        val sykmeldingId =
            sykmeldingIder.firstOrNull { id ->
                advisoryLock.tryAcquire("ImporterteHistoriskeStatuserProsessor", id)
            }
        if (sykmeldingId == null) {
            return Resultat.PROV_IGJEN
        }

        val statuser = tsmHistoriskeSykmeldingstatusDao.lesAlleFor(sykmeldingId)

        for (status in statuser) {
            leggTilStatus(status)
        }
        tsmHistoriskeSykmeldingstatusDao.settAlleLestInnFor(sykmeldingId)
        return Resultat.OK
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
        private val classHash: Int = this::class.qualifiedName?.hashCode() ?: error("Klarte ikke lage en hash for klassenavn")
        private val sisteDato: LocalDate = LocalDate.parse("2020-05-01")
    }

    fun lesNesteSykmeldingIderForBehandling(): List<String> =
        jdbcTemplate.queryForList(
            """
            WITH
            relevante_sykmelding_id AS (
                SELECT DISTINCT ON (sykmelding_id)
                    sykmelding_id,
                    timestamp
                FROM TEMP_TSM_HISTORISK_SYKMELDINGSTATUS
                WHERE 
                    NOT lest_inn
                    AND timestamp <= :sisteDato
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

    fun lesAlleFor(sykmeldingId: String): List<SykmeldingStatusKafkaDTO> =
        jdbcTemplate.query(
            """
            SELECT *
            FROM TEMP_TSM_HISTORISK_SYKMELDINGSTATUS
            WHERE sykmelding_id = :sykmeldingId
            ORDER BY timestamp ASC
            """.trimIndent(),
            mapOf("sykmeldingId" to sykmeldingId),
            TsmSykmeldingerRowMapper,
        )

    fun settAlleLestInnFor(sykmeldingId: String) {
        jdbcTemplate.update(
            """
            UPDATE TEMP_TSM_HISTORISK_SYKMELDINGSTATUS
            SET lest_inn = TRUE
            WHERE sykmelding_id = :sykmeldingId
            """.trimIndent(),
            mapOf("sykmeldingId" to sykmeldingId),
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
