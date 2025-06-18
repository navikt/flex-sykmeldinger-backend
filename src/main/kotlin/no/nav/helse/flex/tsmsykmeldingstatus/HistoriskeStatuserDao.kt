package no.nav.helse.flex.tsmsykmeldingstatus

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.objectMapper
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

interface HistoriskeStatuserDao {
    fun lesNesteSykmeldingIderForBehandling(): List<String>

    fun lesAlleBehandledeSykmeldingIder(): List<String>

    fun lesAlleStatuserForSykmelding(sykmeldingId: String): List<SykmeldingStatusKafkaDTO>

    fun settAlleStatuserForSykmeldingLest(
        sykmeldingId: String,
        tidspunkt: Instant = Instant.now(),
    )
}

@Repository("historiskeStatuserDao")
class HistoriskeStatuserDbDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : HistoriskeStatuserDao {
    companion object {
        private val sisteDato: LocalDate = LocalDate.parse("2020-05-01")
    }

    override fun lesNesteSykmeldingIderForBehandling(): List<String> =
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

    override fun lesAlleBehandledeSykmeldingIder(): List<String> =
        jdbcTemplate.queryForList(
            """
            SELECT sykmelding_id
            FROM temp_tsm_historisk_sykmeldingstatus_innlesing
            """.trimIndent(),
            emptyMap<String, Any>(),
            String::class.java,
        )

    override fun lesAlleStatuserForSykmelding(sykmeldingId: String): List<SykmeldingStatusKafkaDTO> =
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

    override fun settAlleStatuserForSykmeldingLest(
        sykmeldingId: String,
        tidspunkt: Instant,
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
