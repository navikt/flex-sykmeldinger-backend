package no.nav.helse.flex.testconfig.fakes

import java.time.Instant
import java.util.function.Supplier

class NowFactoryFake : Supplier<Instant> {
    private var overrideNow: Instant? = null

    override fun get(): Instant = overrideNow ?: Instant.now()

    fun setNow(now: Instant) {
        this.overrideNow = now
    }

    fun reset() {
        this.overrideNow = null
    }
}
