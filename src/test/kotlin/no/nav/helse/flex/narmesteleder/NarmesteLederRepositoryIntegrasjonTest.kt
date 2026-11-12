package no.nav.helse.flex.narmesteleder

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.junit.jupiter.api.Test

class NarmesteLederRepositoryIntegrasjonTest : IntegrasjonTestOppsett() {
    @Test
    fun `burde hente arbeidsforhold ved bruker fnr`() {
        val narmesteLeder = narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = "1"))
        narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = "2"))
        narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = "3"))

        val alleNarmesteLedere = narmesteLederRepository.findAllByBrukerFnrIn(listOf("1", "2"))
        alleNarmesteLedere shouldHaveSize 2

        val lagretNarmesteLeder = alleNarmesteLedere.first()
        narmesteLeder shouldBe lagretNarmesteLeder
    }
}
