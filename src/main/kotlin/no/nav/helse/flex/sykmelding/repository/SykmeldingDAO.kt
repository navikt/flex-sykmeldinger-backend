package no.nav.helse.flex.sykmelding.db

import no.nav.helse.flex.logger
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfall
import no.nav.helse.flex.objectMapper
// import no.nav.helse.flex.util.tilOsloZone
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tilOsloZone
import java.time.Instant
import java.time.LocalDateTime

@Service
@Transactional
@Repository
class SykmeldingDAO(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val sykmeldingStatusDAO: SykmeldingStatusDAO
) {
    val log = logger()

    fun finnSykmeldinger(identer: List<String>): List<SykmeldingMedBehandlingsutfall> {
        val sykmeldinger = namedParameterJdbcTemplate.query(
            """
            SELECT * FROM sykmelding 
            WHERE fnr IN (:identer)
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("identer", identer),
            sykmeldingRowMapper()
        )

        return populerMedStatus(sykmeldinger)
    }

    fun finnSykmelding(sykmeldingId: String): SykmeldingMedBehandlingsutfall? {
        val sykmeldinger = namedParameterJdbcTemplate.query(
            """
            SELECT * FROM sykmelding 
            WHERE sykmelding_id = :sykmeldingId
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("sykmeldingId", sykmeldingId),
            sykmeldingRowMapper()
        )

        return populerMedStatus(sykmeldinger).firstOrNull()
    }

    fun lagreSykmelding(sykmeldingMedBehandlingsutfall: SykmeldingMedBehandlingsutfall) {
        val parameters = MapSqlParameterSource()
            .addValue("sykmeldingId", sykmeldingMedBehandlingsutfall.sykmelding.id)
            .addValue("fnr", sykmeldingMedBehandlingsutfall.sykmelding.pasient.fnr)
            .addValue("behandlingsutfall", objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.behandlingsutfall))
            .addValue("sykmelding", objectMapper.writeValueAsString(sykmeldingMedBehandlingsutfall.sykmelding))
            .addValue("opprettet", Instant.now().tilOsloZone())

        namedParameterJdbcTemplate.update(
            """
            INSERT INTO sykmelding (
                sykmelding_id, fnr, behandlingsutfall, sykmelding, opprettet
            ) VALUES (
                :sykmeldingId, :fnr, :behandlingsutfall::jsonb, :sykmelding::jsonb, :opprettet
            )
            ON CONFLICT (sykmelding_id) DO NOTHING
            """.trimIndent(),
            parameters
        )
    }

    fun bekreftSykmelding(sykmeldingId: String, bekreftetDato: LocalDateTime) {
        namedParameterJdbcTemplate.update(
            """
            UPDATE sykmelding 
            SET bekreftet_dato = :bekreftetDato
            WHERE sykmelding_id = :sykmeldingId
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("bekreftetDato", bekreftetDato)
                .addValue("sykmeldingId", sykmeldingId)
        )
    }

    private fun sykmeldingRowMapper() = RowMapper { resultSet, _ ->
        val behandlingsutfall: no.nav.helse.flex.sykmelding.domain.Behandlingsutfall =
            objectMapper.readValue(resultSet.getString("behandlingsutfall"))
        val sykmelding: no.nav.helse.flex.sykmelding.domain.Sykmelding =
            objectMapper.readValue(resultSet.getString("sykmelding"))

        SykmeldingMedBehandlingsutfall(
            sykmelding = sykmelding,
            behandlingsutfall = behandlingsutfall
        )
    }

    private fun populerMedStatus(sykmeldinger: List<SykmeldingMedBehandlingsutfall>): List<SykmeldingMedBehandlingsutfall> {
        if (sykmeldinger.isEmpty()) {
            return emptyList()
        }

        val sykmeldingStatuser = sykmeldingStatusDAO.finnStatuser(sykmeldinger.map { it.sykmelding.id }.toSet())

        return sykmeldinger.map { sykmelding ->
            sykmelding.copy(
                // Additional status logic can be added here if needed
            )
        }
    }
}

@Service
@Transactional
@Repository
class SykmeldingStatusDAO(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {

    fun finnStatuser(sykmeldingIds: Set<String>): Map<String, List<SykmeldingStatus>> {
        if (sykmeldingIds.isEmpty()) {
            return emptyMap()
        }

        val statuser = namedParameterJdbcTemplate.query(
            """
            SELECT * FROM sykmeldingstatus 
            WHERE sykmelding_id IN (:sykmeldingIds)
            ORDER BY timestamp DESC
            """.trimIndent(),
            MapSqlParameterSource("sykmeldingIds", sykmeldingIds)
        ) { resultSet, _ ->
            SykmeldingStatus(
                sykmeldingId = resultSet.getString("sykmelding_id"),
                timestamp = resultSet.getTimestamp("timestamp").toInstant(),
                status = resultSet.getString("status"),
                arbeidsgiver = resultSet.getString("arbeidsgiver")?.let { objectMapper.readTree(it) },
                sporsmal = resultSet.getString("sporsmal")?.let { objectMapper.readTree(it) }
            )
        }

        return statuser.groupBy { it.sykmeldingId }
    }

    fun lagreStatus(status: SykmeldingStatus) {
        namedParameterJdbcTemplate.update(
            """
            INSERT INTO sykmeldingstatus (
                sykmelding_id, timestamp, status, arbeidsgiver, sporsmal, opprettet
            ) VALUES (
                :sykmeldingId, :timestamp, :status, :arbeidsgiver::jsonb, :sporsmal::jsonb, :opprettet
            )
            """.trimIndent(),
            MapSqlParameterSource()
                .addValue("sykmeldingId", status.sykmeldingId)
                .addValue("timestamp", status.timestamp.tilOsloZone())
                .addValue("status", status.status)
                .addValue("arbeidsgiver", status.arbeidsgiver?.toString())
                .addValue("sporsmal", status.sporsmal?.toString())
                .addValue("opprettet", Instant.now().tilOsloZone())
        )
    }
}

data class SykmeldingStatus(
    val sykmeldingId: String,
    val timestamp: Instant,
    val status: String,
    val arbeidsgiver: com.fasterxml.jackson.databind.JsonNode?,
    val sporsmal: com.fasterxml.jackson.databind.JsonNode?
)
