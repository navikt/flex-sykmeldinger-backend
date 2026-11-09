package no.nav.helse.flex.narmesteleder

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class NarmesteLederEsyfoCheckpoint(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentPartisjoner(topic: String): List<Int> =
        jdbcTemplate
            .query(
                "SELECT partition FROM narmeste_leder_esyfo_checkpoint WHERE topic = ?",
                { rs, _ -> rs.getInt(1) },
                topic,
            ).orEmpty()

    fun hentOffset(
        topic: String,
        partition: Int,
    ): Long? =
        jdbcTemplate
            .query(
                """SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint
                   WHERE topic = ? AND partition = ?""",
                { rs, _ -> rs.getLong(1) },
                topic,
                partition,
            ).singleOrNull()

    // INSERT låser også den første raden ved samtidig oppstart. SELECT låser raden frem til commit.
    fun lasOgHentOffset(
        topic: String,
        partition: Int,
    ): Long {
        jdbcTemplate.update(
            """INSERT INTO narmeste_leder_esyfo_checkpoint (topic, partition)
               VALUES (?, ?) ON CONFLICT (topic, partition) DO NOTHING""",
            topic,
            partition,
        )
        return jdbcTemplate.queryForObject(
            """SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint
               WHERE topic = ? AND partition = ? FOR UPDATE""",
            Long::class.java,
            topic,
            partition,
        ) ?: error("Mangler checkpoint")
    }

    fun lagreOffset(
        topic: String,
        partition: Int,
        offset: Long,
    ) {
        check(
            jdbcTemplate.update(
                """UPDATE narmeste_leder_esyfo_checkpoint SET offset_sist_pafort = ?
                   WHERE topic = ? AND partition = ?""",
                offset,
                topic,
                partition,
            ) == 1,
        ) { "Kunne ikke lagre checkpoint" }
    }
}
