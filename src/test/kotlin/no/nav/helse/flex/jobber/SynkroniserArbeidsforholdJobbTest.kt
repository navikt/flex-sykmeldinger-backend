package no.nav.helse.flex.jobber

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.arbeidsforhold.manuellsynk.SynkroniserArbeidsforhold
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.TempSynkroniserArbeidsforholdRepositoryFake
import org.amshove.kluent.`should be false`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SynkroniserArbeidsforholdJobbTest : FakesTestOppsett() {
    @Autowired
    private lateinit var tempSynkroniserArbeidsforholdRepository: TempSynkroniserArbeidsforholdRepositoryFake

    @Autowired
    private lateinit var jobb: SynkroniserArbeidsforholdJobb

    @Autowired
    private lateinit var aaregClientFake: AaregClientFake

    @AfterEach
    fun cleanUp() {
        tempSynkroniserArbeidsforholdRepository.deleteAll()
        aaregClientFake.reset()
    }

    @Test
    fun `burde hente arbeidsforhold`() {
        aaregClientFake.setArbeidsforholdoversikt(lagArbeidsforholdOversiktResponse())

        tempSynkroniserArbeidsforholdRepository.saveAll(
            mutableListOf(
                SynkroniserArbeidsforhold(fnr = "1"),
            ),
        )

        jobb.kjørJobb()

        arbeidsforholdRepository.findAll().shouldHaveSize(1)
        tempSynkroniserArbeidsforholdRepository
            .findAll()
            .single()
            .lest
            .`should be false`()
    }
}
