package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
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
        narmesteLeder `should be equal to` lagretNarmesteLeder
    }
}
