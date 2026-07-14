package no.nav.helse.flex.outbox

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface OutboxDbRepository : CrudRepository<OutboxDbRecord, Long> {
    /**
     * Finner alle usendte outbox-rader for det eldste ledige fnr-et, og tar en
     * transaksjons-scoped advisory lock på fnr-et slik at ingen andre instanser
     * plukker samme fnr samtidig.
     *
     * Advisory-låsen holdes til transaksjonen committer/ruller tilbake. Andre
     * instanser hopper over fnr som allerede er låst (pg_try_advisory_xact_lock
     * gir false), og velger neste ledige fnr i stedet.
     *
     * Rekkefølgen bevares slik at meldinger for samme fnr sendes i opprettet-rekkefølge.
     * id (monotont økende sekvens) brukes som tie-breaker når flere rader deler
     * opprettet_timestamp, slik at rekkefølgen alltid er deterministisk (innsettingsrekkefølge).
     * Tilsvarende velges eldste ledige fnr deterministisk (eldste, deretter laveste id) når
     * flere fnr deler samme eldste opprettet_timestamp.
     */
    @Query(
        """
    SELECT *
    FROM outbox
    WHERE sendt_timestamp IS NULL
      AND fnr = (
          SELECT fnr
          FROM (
              SELECT fnr, MIN(opprettet_timestamp) AS eldste, MIN(id) AS eldste_id
              FROM outbox
              WHERE sendt_timestamp IS NULL
              GROUP BY fnr
              ORDER BY eldste, eldste_id
          ) kandidater
          WHERE pg_try_advisory_xact_lock(hashtext(fnr))
          LIMIT 1
      )
    ORDER BY opprettet_timestamp, id
    """,
    )
    fun finnOgLasUsendteForEldsteLedigeFnr(): List<OutboxDbRecord>
}
