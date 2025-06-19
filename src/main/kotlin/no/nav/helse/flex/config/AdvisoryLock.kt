package no.nav.helse.flex.config

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

interface AdvisoryLock {
    fun acquire(
        key1: String,
        key2: String,
    )

    fun acquire(key: String)

    fun tryAcquire(
        key1: String,
        key2: String,
    ): Boolean

    fun tryAcquire(key: String): Boolean
}

@Component("advisoryLock")
class DatabaseAdvisoryLock(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : AdvisoryLock {
    override fun acquire(
        key1: String,
        key2: String,
    ) {
        acquire(stringToIntHash(key1), stringToIntHash(key2))
    }

    override fun acquire(key: String) {
        acquire(stringToLongHash(key))
    }

    override fun tryAcquire(key: String): Boolean = tryAcquire(stringToLongHash(key))

    override fun tryAcquire(
        key1: String,
        key2: String,
    ): Boolean = tryAcquire(stringToIntHash(key1), stringToIntHash(key2))

    companion object {
        fun stringToIntHash(value: String): Int = value.hashCode()

        fun stringToLongHash(value: String): Long = value.hashCode().toLong()
    }

    internal fun acquire(
        key1: Int,
        key2: Int,
    ) {
        jdbcTemplate.queryForObject(
            "SELECT pg_advisory_xact_lock(:key1, :key2)",
            mapOf("key1" to key1, "key2" to key2),
            Any::class.java,
        )
    }

    internal fun acquire(key: Long) {
        jdbcTemplate.queryForObject(
            "SELECT pg_advisory_xact_lock(:key)",
            mapOf("key" to key),
            Any::class.java,
        )
    }

    internal fun tryAcquire(
        key1: Int,
        key2: Int,
    ): Boolean =
        jdbcTemplate.queryForObject(
            "SELECT pg_try_advisory_xact_lock(:key1, :key2)",
            mapOf("key1" to key1, "key2" to key2),
            Boolean::class.java,
        ) ?: error("Kunne ikke hente resultat av pg_try_advisory_xact_lock")

    internal fun tryAcquire(key: Long): Boolean =
        jdbcTemplate.queryForObject(
            "SELECT pg_try_advisory_xact_lock(:key)",
            mapOf("key" to key),
            Boolean::class.java,
        ) ?: error("Kunne ikke hente resultat av pg_try_advisory_xact_lock")
}
