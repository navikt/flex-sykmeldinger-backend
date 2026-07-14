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
     * Kandidatene materialiseres (WITH ... AS MATERIALIZED) og sorteres FØR
     * advisory-låsen tas. Det er avgjørende: uten materialisering pusher Postgres
     * pg_try_advisory_xact_lock ned i aggregeringen og evaluerer den for ALLE
     * kandidat-fnr, slik at én instans låser samtlige ledige fnr og de andre
     * ikke får noe. Med materialisering evalueres låsen på den ferdig sorterte
     * lista, i rekkefølge, og stopper på første ledige fnr (LIMIT 1) – da låses
     * kun det valgte fnr-et.
     *
     * Rekkefølgen bevares slik at meldinger for samme fnr sendes i opprettet-rekkefølge.
     * id (monotont økende sekvens) brukes som tie-breaker når flere rader deler
     * opprettet_timestamp, slik at rekkefølgen alltid er deterministisk (innsettingsrekkefølge).
     * Tilsvarende velges eldste ledige fnr deterministisk (eldste, deretter laveste id) når
     * flere fnr deler samme eldste opprettet_timestamp.
     */
    @Query(
        """
    WITH kandidater AS MATERIALIZED (
        SELECT fnr
        FROM outbox
        WHERE sendt_timestamp IS NULL
        GROUP BY fnr
        ORDER BY MIN(opprettet_timestamp), MIN(id)
    )
    SELECT *
    FROM outbox
    WHERE sendt_timestamp IS NULL
      AND fnr = (
          SELECT fnr
          FROM kandidater
          WHERE pg_try_advisory_xact_lock(hashtext(fnr))
          LIMIT 1
      )
    ORDER BY opprettet_timestamp, id
    """,
    )
    fun finnOgLasUsendteForEldsteLedigeFnr(): List<OutboxDbRecord>
}
