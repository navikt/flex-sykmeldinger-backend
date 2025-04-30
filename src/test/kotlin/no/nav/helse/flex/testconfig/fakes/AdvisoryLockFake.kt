package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.config.AdvisoryLock

class AdvisoryLockFake : AdvisoryLock {
    override fun acquire(
        key1: String,
        key2: String,
    ) {
        return
    }

    override fun acquire(key: String) {
        return
    }
}
