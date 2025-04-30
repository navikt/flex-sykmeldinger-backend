package no.nav.helse.flex.config

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.support.TransactionTemplate

class DatabaseAdvisoryLockIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    private lateinit var advisoryLock: DatabaseAdvisoryLock

    @Autowired
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate

    @Autowired
    private lateinit var txTemplate: TransactionTemplate

    @Test
    fun `burde låse med én key`() {
        txTemplate.execute {
            advisoryLock.acquire("test")
            val hashedKey = DatabaseAdvisoryLock.stringToLongHash("test")
            checkAdvisoryLockExists(hashedKey).shouldBeTrue()
        }
    }

    @Test
    fun `burde låse med to keys`() {
        txTemplate.execute {
            advisoryLock.acquire("test1", "test2")
            val hashedKey1 = DatabaseAdvisoryLock.stringToIntHash("test1")
            val hashedKey2 = DatabaseAdvisoryLock.stringToIntHash("test2")
            checkAdvisoryLockExists(hashedKey1, hashedKey2).shouldBeTrue()
        }
    }

    @Test
    fun `burde slippe lås etter transaksjon`() {
        txTemplate.execute {
            advisoryLock
                .acquire("test")
        }
        val hashedKey = DatabaseAdvisoryLock.stringToLongHash("test")
        checkAdvisoryLockExists(hashedKey).shouldBeFalse()
    }

    private fun listAdvisoryLocks(): List<Map<String, Any>> =
        jdbcTemplate.queryForList(
            """
                SELECT *
                FROM pg_locks 
                WHERE locktype = 'advisory'
                    AND granted = true
                """,
            mapOf<String, Any>(),
        )

    private fun checkAdvisoryLockExists(key: Long): Boolean = checkAdvisoryLockGranted(classid = 0, objid = key)

    private fun checkAdvisoryLockExists(
        key1: Int,
        key2: Int,
    ): Boolean = checkAdvisoryLockGranted(classid = key1, objid = key2)

    private fun checkAdvisoryLockGranted(
        classid: Any,
        objid: Any,
    ): Boolean =
        jdbcTemplate.queryForObject(
            """
                SELECT EXISTS(
                    SELECT 1
                    FROM pg_locks 
                    WHERE locktype = 'advisory'
                        AND classid = :classid
                        AND objid = :objid
                        AND granted = true
                );
                """,
            mapOf("classid" to classid, "objid" to objid),
            Boolean::class.java,
        ) ?: false
}
