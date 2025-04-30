package no.nav.helse.flex.arbeidsgiverdetaljer

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

class ArbeidsgiverDetaljerHenterFakeTest : FakesTestOppsett() {
    @Autowired
    lateinit var arbeidsgiverDetaljerService: ArbeidsgiverDetaljerService

    @AfterEach
    fun etterHver() {
        slettDatabase()
    }

    @Test
    fun `burde hente en arbeidsgiverDetaljer`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", orgnummer = "org1"),
        )
        narmesteLederRepository.save(
            lagNarmesteLeder(brukerFnr = "1", orgnummer = "org1"),
        )

        val arbeidsgiverDetaljer = arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))

        arbeidsgiverDetaljer.size `should be equal to` 1
        arbeidsgiverDetaljer.first().naermesteLeder.shouldNotBeNull()
    }

    @Test
    fun `burde hente en arbeidsgiverDetaljer uten narmeste leder`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", orgnummer = "org1"),
        )

        val arbeidsgiverDetaljer = arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))

        arbeidsgiverDetaljer.size `should be equal to` 1
        arbeidsgiverDetaljer.first().naermesteLeder.shouldBeNull()
    }

    @Test
    fun `burde hente riktig arbeidsgiverDetaljer for person`() {
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

        val alleArbeidsgiverDetaljer = arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))

        alleArbeidsgiverDetaljer.size `should be equal to` 1
        val arbeidsgiverDetaljer = alleArbeidsgiverDetaljer.first()
        arbeidsgiverDetaljer.orgnummer `should be equal to` "org1"
        arbeidsgiverDetaljer.naermesteLeder
            .shouldNotBeNull()
            .orgnummer `should be equal to` "org1"
    }

    @Test
    fun `burde hente arbeidsgiverDetaljer innenfor periode`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", fom = LocalDate.parse("2021-01-01"), tom = LocalDate.parse("2021-02-01")),
        )

        val arbeidsgiverDetaljer =
            arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(
                PersonIdenter("1"),
                periode =
                    LocalDate.parse("2021-01-01") to LocalDate.parse("2021-02-01"),
            )

        arbeidsgiverDetaljer.size `should be equal to` 1
    }

    @Test
    fun `burde ikke hente arbeidsgiverDetaljer utenfor periode`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", fom = LocalDate.parse("2021-01-01"), tom = LocalDate.parse("2021-02-01")),
        )

        val arbeidsgiverDetaljer =
            arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(
                PersonIdenter("1"),
                periode =
                    LocalDate.parse("2021-03-01") to LocalDate.parse("2021-04-01"),
            )

        arbeidsgiverDetaljer.size `should be equal to` 0
    }
}
