package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.narmesteleder.domain.NarmesteLederEsyfoOffset
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederEsyfoOffsetId
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface NarmesteLederEsyfoOffsetRepository : CrudRepository<NarmesteLederEsyfoOffset, NarmesteLederEsyfoOffsetId> {
    @Modifying
    @Query(
        """
        INSERT INTO narmeste_leder_esyfo_checkpoint (topic, partition)
        VALUES (:topic, :partition) ON CONFLICT (topic, partition) DO NOTHING
        """,
    )
    fun opprettCheckpoint(
        topic: String,
        partition: Int,
    ): Int

    @Query(
        """
        SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint
        WHERE topic = :topic AND partition = :partition FOR UPDATE
        """,
    )
    fun hentOffsetForOppdatering(
        topic: String,
        partition: Int,
    ): Long?
}
