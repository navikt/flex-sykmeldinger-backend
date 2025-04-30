package no.nav.helse.flex.virksomhet

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.testconfig.FakesTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class VirksomhetHenterFakeTest : FakesTestOppsett() {
    @Autowired
    lateinit var virksomhetHenterService: VirksomhetHenterService

    @AfterEach
    fun etterHver() {
        slettDatabase()
    }

    @Test
    fun `burde hente en virksomhet`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", orgnummer = "org1"),
        )
        narmesteLederRepository.save(
            lagNarmesteLeder(brukerFnr = "1", orgnummer = "org1"),
        )

        val virksomheter = virksomhetHenterService.hentVirksomheterForPerson(PersonIdenter("1"))

        virksomheter.size `should be equal to` 1
        virksomheter.first().naermesteLeder.shouldNotBeNull()
    }

    @Test
    fun `burde hente en virksomhet uten narmeste leder`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", orgnummer = "org1"),
        )

        val virksomheter = virksomhetHenterService.hentVirksomheterForPerson(PersonIdenter("1"))

        virksomheter.size `should be equal to` 1
        virksomheter.first().naermesteLeder.shouldBeNull()
    }

    @Test
    fun `burde hente riktig virksomhet for person`() {
        arbeidsforholdRepository.saveAll(
            listOf(
                lagArbeidsforhold(fnr = "1", orgnummer = "org1"),
                lagArbeidsforhold(fnr = "2", orgnummer = "org2"),
            ),
        )
        narmesteLederRepository.saveAll(
            listOf(
                lagNarmesteLeder(brukerFnr = "1", orgnummer = "org1"),
                lagNarmesteLeder(brukerFnr = "2", orgnummer = "org2"),
            ),
        )

        val virksomheter = virksomhetHenterService.hentVirksomheterForPerson(PersonIdenter("1"))

        virksomheter.size `should be equal to` 1
        val virksomhet = virksomheter.first()
        virksomhet.orgnummer `should be equal to` "org1"
        virksomhet.naermesteLeder
            .shouldNotBeNull()
            .orgnummer `should be equal to` "org1"
    }

    @Test
    fun `burde hente virksomheter innenfor periode`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", fom = LocalDate.parse("2021-01-01"), tom = LocalDate.parse("2021-02-01")),
        )

        val virksomheter =
            virksomhetHenterService.hentVirksomheterForPerson(
                PersonIdenter("1"),
                periode =
                    LocalDate.parse("2021-01-01") to LocalDate.parse("2021-02-01"),
            )

        virksomheter.size `should be equal to` 1
    }

    @Test
    fun `burde ikke hente virksomheter utenfor periode`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", fom = LocalDate.parse("2021-01-01"), tom = LocalDate.parse("2021-02-01")),
        )

        val virksomheter =
            virksomhetHenterService.hentVirksomheterForPerson(
                PersonIdenter("1"),
                periode =
                    LocalDate.parse("2021-03-01") to LocalDate.parse("2021-04-01"),
            )

        virksomheter.size `should be equal to` 0
    }
}
