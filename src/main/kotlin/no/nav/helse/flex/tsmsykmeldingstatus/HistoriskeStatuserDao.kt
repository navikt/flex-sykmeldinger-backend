package no.nav.helse.flex.tsmsykmeldingstatus

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.objectMapper
import org.postgresql.util.PGobject
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.OffsetDateTime

interface HistoriskeStatuserDao {
    fun lesAlleStatuserEldstTilNyest(
        fraTimestamp: Instant,
        tilTimestamp: Instant,
        antall: Int = 1000,
    ): List<SykmeldingStatusKafkaDTO>

    fun lesCheckpointStatusTimestamp(): Instant?

    fun oppdaterCheckpointStatusTimestamp(statusTimestamp: Instant)

    fun lesAlleMedId(sykmeldingIder: Iterable<String>): List<SykmeldingStatusKafkaDTO>
}

@Repository("historiskeStatuserDao")
class HistoriskeStatuserDbDao(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : HistoriskeStatuserDao {
    override fun lesAlleStatuserEldstTilNyest(
        fraTimestamp: Instant,
        tilTimestamp: Instant,
        antall: Int,
    ): List<SykmeldingStatusKafkaDTO> =
        jdbcTemplate.query(
            """
            SELECT *
            FROM temp_tsm_historisk_sykmeldingstatus
            WHERE 
                timestamp > :fraTimestamp
                and timestamp <= :tilTimestamp
            ORDER BY timestamp ASC
            LIMIT :antall
            """.trimIndent(),
            mapOf(
                "fraTimestamp" to Timestamp.from(fraTimestamp),
                "tilTimestamp" to Timestamp.from(tilTimestamp),
                "antall" to antall,
            ),
            TsmSykmeldingerRowMapper,
        )

    override fun lesCheckpointStatusTimestamp(): Instant? =
        jdbcTemplate
            .query(
                """
                select status_timestamp
                FROM temp_tsm_historisk_sykmeldingstatus_checkpoint
                where id = '1'
                limit 1
                """.trimIndent(),
                emptyMap<String, Any>(),
            ) { rs, _ -> rs.getTimestamp("status_timestamp").toInstant() }
            .firstOrNull()

    override fun oppdaterCheckpointStatusTimestamp(statusTimestamp: Instant) {
        jdbcTemplate.update(
            """
            INSERT INTO temp_tsm_historisk_sykmeldingstatus_checkpoint (id, status_timestamp)
            VALUES ('1', :statusTimestamp)
            ON CONFLICT (id)
                DO UPDATE SET
                    status_timestamp = EXCLUDED.status_timestamp
            """.trimIndent(),
            mapOf("statusTimestamp" to Timestamp.from(statusTimestamp)),
        )
    }

    override fun lesAlleMedId(sykmeldingIder: Iterable<String>): List<SykmeldingStatusKafkaDTO> =
        jdbcTemplate.query(
            """
            SELECT *
            FROM temp_tsm_historisk_sykmeldingstatus
            WHERE sykmelding_id IN (:sykmeldingIder)
            """.trimIndent(),
            mapOf(
                "sykmeldingIder" to sykmeldingIder,
            ),
            TsmSykmeldingerRowMapper,
        )
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
