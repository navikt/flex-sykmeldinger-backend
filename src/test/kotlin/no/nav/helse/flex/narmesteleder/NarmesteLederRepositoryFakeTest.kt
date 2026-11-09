package no.nav.helse.flex.narmesteleder

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class NarmesteLederRepositoryFakeTest {
    @Test
    fun `fake repository lagrer oppdatering og sletter aktiv rad`() {
        val repository = NarmesteLederRepositoryFake()
        val opprinnelig = lagNarmesteLeder()
        repository.save(opprinnelig)
        val lagret = repository.findByNarmesteLederId(opprinnelig.narmesteLederId).shouldNotBeNull()
        lagret.id.shouldNotBeNull()
        val oppdatert = repository.save(lagret.copy(timestamp = lagret.timestamp.minusSeconds(1), orgnummer = "888888"))
        repository.findByNarmesteLederId(lagret.narmesteLederId) shouldBe oppdatert
        oppdatert.id shouldBe lagret.id
        repository.count() shouldBe 1
        repository.delete(oppdatert)
        repository.findByNarmesteLederId(lagret.narmesteLederId) shouldBe null
    }
}
