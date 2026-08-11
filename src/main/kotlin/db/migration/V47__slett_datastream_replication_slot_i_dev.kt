package db.migration

import kotlin.use

@Suppress("ktlint:standard:class-naming")
class V47__slett_datastream_replication_slot_i_dev : org.flywaydb.core.api.migration.BaseJavaMigration() {
    override fun migrate(context: org.flywaydb.core.api.migration.Context) {
        val kanSlettes =
            context.connection
                .prepareStatement("SELECT active FROM pg_replication_slots WHERE slot_name = ?")
                .use { statement ->
                    statement.setString(1, "flex_sykmeldinger_backend_replication")
                    statement.executeQuery().use { resultSet ->
                        if (!resultSet.next()) {
                            return
                        }
                        resultSet.getBoolean("active")
                    }
                }

        kotlin.check(!kanSlettes) {
            "Kan ikke slette replication slot flex_sykmeldinger_backend_replication fordi den er aktiv"
        }

        context.connection
            .prepareStatement(
                "SELECT pg_drop_replication_slot(?)",
            ).use { statement ->
                statement.setString(1, "flex_sykmeldinger_backend_replication")
                statement.execute()
            }
    }
}
