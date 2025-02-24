package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.testconfig.FellesTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class NarmesteLederRepositoryIntegrasjonTest : FellesTestOppsett() {
    @Test
    fun `burde hente arbeidsforhold ved bruker fnr`() {
        val narmesteLeder = narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = "1"))

        val alleNarmesteLedere = narmesteLederRepository.findAllByBrukerFnr("1")
        alleNarmesteLedere shouldHaveSize 1

        val lagretNarmesteLeder = alleNarmesteLedere.first()
        narmesteLeder `should be equal to` lagretNarmesteLeder
    }
}
