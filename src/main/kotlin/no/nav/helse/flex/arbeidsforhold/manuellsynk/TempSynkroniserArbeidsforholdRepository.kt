package no.nav.helse.flex.arbeidsforhold.manuellsynk

import org.springframework.data.annotation.Id
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.relational.core.mapping.Table
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

private const val TABLE_NAME = "temp_synkroniser_arbeidsforhold"

@Table(TABLE_NAME)
data class SynkroniserArbeidsforhold(
    @Id
    val id: String? = null,
    val fnr: String,
    val lest: Boolean = false,
)

@Repository
interface TempSynkroniserArbeidsforholdRepository : CrudRepository<SynkroniserArbeidsforhold, String> {
    @Query(
        """
        SELECT *
        FROM $TABLE_NAME
        WHERE lest = false
        FOR UPDATE SKIP LOCKED
        LIMIT :batchSize
    """,
    )
    fun findNextBatch(
        @Param("batchSize") batchSize: Int,
    ): List<SynkroniserArbeidsforhold>
}
