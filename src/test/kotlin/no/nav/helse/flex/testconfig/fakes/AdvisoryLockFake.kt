package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.config.AdvisoryLock

class AdvisoryLockFake : AdvisoryLock {
    private val heldLocks: MutableMap<FakeLock, Int> = mutableMapOf()

    override fun acquire(
        key1: String,
        key2: String,
    ) {
        internalAcquire(FakeLock(key1, key2))
    }

    override fun acquire(key: String) {
        internalAcquire(FakeLock(key, null))
    }

    fun hasLocks(): Boolean = lockCount() > 0

    fun hasLock(key: String): Boolean = lockCount(key) > 0

    fun hasLock(
        key1: String,
        key2: String,
    ): Boolean = lockCount(key1, key2) > 0

    fun lockCount(): Int = heldLocks.values.sum()

    fun lockCount(key: String): Int = heldLocks.getOrDefault(FakeLock(key, null), 0)

    fun lockCount(
        key1: String,
        key2: String,
    ): Int = heldLocks.getOrDefault(FakeLock(key1, key2), 0)

    fun reset() {
        heldLocks.clear()
    }

    private fun internalAcquire(lock: FakeLock) {
        heldLocks.merge(lock, 1) { oldValue, _ ->
            oldValue + 1
        }
    }
}

data class FakeLock(
    val key1: String,
    val key2: String?,
)
