package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.narmesteleder.NarmesteLederEsyfoCheckpoint

class NarmesteLederEsyfoCheckpointFake : NarmesteLederEsyfoCheckpoint {
    private val offseter = mutableMapOf<Pair<String, Int>, Long>()

    override fun lasOgHentOffset(
        topic: String,
        partition: Int,
    ): Long = offseter.getOrDefault(topic to partition, -1L)

    override fun lagreOffset(
        topic: String,
        partition: Int,
        offset: Long,
    ) {
        offseter[topic to partition] = offset
    }

    fun reset() {
        offseter.clear()
    }
}
