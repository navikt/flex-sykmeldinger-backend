package no.nav.helse.flex.config

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

interface AdvisoryLock {
    fun acquire(
        key1: String,
        key2: String,
    )

    fun acquire(key: String)
}

@Component("advisoryLock")
class DatabaseAdvisoryLock(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : AdvisoryLock {
    @Transactional(rollbackFor = [Exception::class])
    override fun acquire(
        key1: String,
        key2: String,
    ) {
        acquire(stringToIntHash(key1), stringToIntHash(key2))
    }

    @Transactional(rollbackFor = [Exception::class])
    override fun acquire(key: String) {
        acquire(stringToLongHash(key))
    }

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
}
