package no.nav.helse.flex.arbeidsgiverdetaljer

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.NarmesteLederLesemodus
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederEsyfo
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.testconfig.FakesTestOppsett
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.util.function.Supplier

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

        arbeidsgiverDetaljer.size shouldBe 1
        arbeidsgiverDetaljer.first().naermesteLeder.shouldNotBeNull()
    }

    @Test
    fun `lesemodus velger kun én tabell og filtrerer inaktive ESYFO-ledere`() {
        arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "1", orgnummer = "org1"))
        narmesteLederRepository.save(lagNarmesteLeder(brukerFnr = "1", orgnummer = "org1"))
        val gammel = arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))
        gammel.single().naermesteLeder.shouldNotBeNull()

        val nyTjeneste =
            ArbeidsgiverDetaljerService(
                arbeidsforholdRepository,
                narmesteLederRepository,
                narmesteLederEsyfoRepository,
                NarmesteLederLesemodus("ESYFO", true),
                Supplier { Instant.now() },
            )
        nyTjeneste
            .hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))
            .single()
            .naermesteLeder
            .shouldBeNull()

        val gammelLeder = lagNarmesteLeder(brukerFnr = "1", orgnummer = "org1")
        val ny =
            NarmesteLederEsyfo(
                oppdatert = gammelLeder.oppdatert,
                timestamp = gammelLeder.timestamp,
                narmesteLederId = gammelLeder.narmesteLederId,
                orgnummer = gammelLeder.orgnummer,
                brukerFnr = gammelLeder.brukerFnr,
                narmesteLederFnr = gammelLeder.narmesteLederFnr,
                aktivFom = gammelLeder.aktivFom,
                narmesteLederNavn = null,
            )
        narmesteLederEsyfoRepository.save(ny)
        nyTjeneste
            .hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))
            .single()
            .naermesteLeder
            .shouldBeNull()
        narmesteLederEsyfoRepository.deleteAll()
        val inaktiv = narmesteLederEsyfoRepository.save(ny.copy(narmesteLederNavn = "Ny leder", aktivTom = LocalDate.now()))
        nyTjeneste
            .hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))
            .single()
            .naermesteLeder
            .shouldBeNull()
        narmesteLederEsyfoRepository.save(inaktiv.copy(aktivTom = null))
        nyTjeneste
            .hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))
            .single()
            .naermesteLeder
            .shouldNotBeNull()
    }

    @Test
    fun `burde hente en arbeidsgiverDetaljer uten narmeste leder`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(fnr = "1", orgnummer = "org1"),
        )

        val arbeidsgiverDetaljer = arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))

        arbeidsgiverDetaljer.size shouldBe 1
        arbeidsgiverDetaljer.first().naermesteLeder.shouldBeNull()
    }

    @Test
    fun `henter aktiv leder selv om en nyere leder er inaktiv`() {
        arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "1", orgnummer = "org1"))
        val aktiv =
            narmesteLederRepository.save(
                lagNarmesteLeder(brukerFnr = "1", orgnummer = "org1", aktivFom = LocalDate.parse("2022-01-01")),
            )
        val inaktiv =
            narmesteLederRepository.save(
                lagNarmesteLeder(brukerFnr = "1", orgnummer = "org1", aktivFom = LocalDate.parse("2023-01-01"))
                    .copy(aktivTom = LocalDate.parse("2024-01-01")),
            )

        val detaljer = arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))

        detaljer.single().naermesteLeder shouldBe aktiv
        narmesteLederRepository.findAllByBrukerFnrIn(listOf("1")).toSet() shouldBe setOf(aktiv, inaktiv)
    }

    @Test
    fun `burde hente riktig arbeidsgiverDetaljer for person`() {
        arbeidsforholdRepository.saveAll(
            listOf(
                lagArbeidsforhold(fnr = "1", orgnummer = "org1", navArbeidsforholdId = "1"),
                lagArbeidsforhold(fnr = "2", orgnummer = "org2", navArbeidsforholdId = "2"),
            ),
        )
        narmesteLederRepository.saveAll(
            listOf(
                lagNarmesteLeder(brukerFnr = "1", orgnummer = "org1"),
                lagNarmesteLeder(brukerFnr = "2", orgnummer = "org2"),
            ),
        )

        val alleArbeidsgiverDetaljer = arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(PersonIdenter("1"))

        alleArbeidsgiverDetaljer.size shouldBe 1
        val arbeidsgiverDetaljer = alleArbeidsgiverDetaljer.first()
        arbeidsgiverDetaljer.orgnummer shouldBe "org1"
        arbeidsgiverDetaljer.naermesteLeder
            .shouldNotBeNull()
            .orgnummer shouldBe "org1"
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

        arbeidsgiverDetaljer.size shouldBe 1
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

        arbeidsgiverDetaljer.size shouldBe 0
    }
}
