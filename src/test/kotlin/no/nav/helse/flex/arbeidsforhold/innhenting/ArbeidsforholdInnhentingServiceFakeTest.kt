package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.gateways.ereg.Navn
import no.nav.helse.flex.gateways.ereg.Nokkelinfo
import no.nav.helse.flex.gateways.pdl.PdlIdent
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EregClientFake
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ArbeidsforholdInnhentingServiceFakeTest : FakesTestOppsett() {
    @Autowired
    lateinit var arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService

    @Autowired
    lateinit var aaregClientFake: AaregClientFake

    @Autowired
    lateinit var eregClientFake: EregClientFake

    @Autowired
    lateinit var pdlClient: PdlClientFake

    @AfterEach
    fun tearDown() {
        slettDatabase()
        aaregClientFake.reset()
        eregClientFake.reset()
        pdlClient.reset()
    }

    @Test
    fun `burde opprette arbeidsforhold som ikke finnes fra før`() {
        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "navArbeidsforholdId",
                        arbeidstakerIdenter = listOf("fnr"),
                    ),
                ),
            ),
            fnr = "fnr",
        )
        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("fnr")
        arbeidsforholdRepository.getAllByFnrIn(listOf("fnr")) shouldHaveSize 1
    }

    @Test
    fun `burde oppdatert arbeidsforhold som finnes fra før`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(
                navArbeidsforholdId = "1",
                fnr = "fnr",
                orgnummer = "org-1",
            ),
        )
        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "1",
                        arbeidstakerIdenter = listOf("fnr"),
                        arbeidsstedOrgnummer = "org-2",
                    ),
                ),
            ),
            fnr = "fnr",
        )
        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("fnr")
        arbeidsforholdRepository
            .getAllByFnrIn(listOf("fnr"))
            .shouldHaveSize(1)
            .first()
            .run {
                orgnummer shouldBeEqualTo "org-2"
            }
    }

    @Test
    fun `lagrer riktig data for nytt arbeidsforhold`() {
        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "1",
                        typeKode = "maritimtArbeidsforhold",
                        arbeidstakerIdenter = listOf("fnr"),
                        arbeidsstedOrgnummer = "orgnummer",
                        opplysningspliktigOrgnummer = "juridiskOrgnummer",
                        startdato = LocalDate.parse("2020-01-01"),
                        sluttdato = null,
                    ),
                ),
            ),
            fnr = "fnr",
        )
        eregClientFake.setNokkelinfo(
            Nokkelinfo(
                navn = Navn("Orgnavn"),
            ),
            orgnummer = "orgnummer",
        )
        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("fnr")
        arbeidsforholdRepository
            .findByNavArbeidsforholdId("1")
            .shouldNotBeNull()
            .run {
                fnr shouldBeEqualTo "fnr"
                orgnummer shouldBeEqualTo "orgnummer"
                juridiskOrgnummer shouldBeEqualTo "juridiskOrgnummer"
                orgnavn shouldBeEqualTo "Orgnavn"
                arbeidsforholdType shouldBeEqualTo ArbeidsforholdType.MARITIMT_ARBEIDSFORHOLD
                fom shouldBeEqualTo LocalDate.parse("2020-01-01")
                tom shouldBeEqualTo null
            }
    }

    @Test
    fun `lagrer riktig data for oppdatert arbeidsforhold`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(
                navArbeidsforholdId = "1",
                fnr = "fnr",
                orgnummer = "orgnummer",
                juridiskOrgnummer = "juridiskOrgnummer",
                orgnavn = "Orgnavn",
                fom = LocalDate.parse("2020-01-01"),
                tom = null,
                arbeidsforholdType = ArbeidsforholdType.MARITIMT_ARBEIDSFORHOLD,
            ),
        )
        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "1",
                        typeKode = "ordinaertArbeidsforhold",
                        arbeidstakerIdenter = listOf("fnr"),
                        arbeidsstedOrgnummer = "orgnummer-2",
                        opplysningspliktigOrgnummer = "juridiskOrgnummer-2",
                        startdato = LocalDate.parse("2020-01-02"),
                        sluttdato = null,
                    ),
                ),
            ),
            fnr = "fnr",
        )
        eregClientFake.setNokkelinfo(
            Nokkelinfo(
                navn = Navn("Orgnavn-2"),
            ),
            orgnummer = "orgnummer-2",
        )
        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("fnr")
        arbeidsforholdRepository
            .findByNavArbeidsforholdId("1")
            .shouldNotBeNull()
            .run {
                fnr shouldBeEqualTo "fnr"
                orgnummer shouldBeEqualTo "orgnummer-2"
                juridiskOrgnummer shouldBeEqualTo "juridiskOrgnummer-2"
                orgnavn shouldBeEqualTo "Orgnavn-2"
                arbeidsforholdType shouldBeEqualTo ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
                fom shouldBeEqualTo LocalDate.parse("2020-01-02")
                tom shouldBeEqualTo null
            }
    }

    @Test
    fun `burde opprette et arbeidsforhold når personen har endret ident og fått nytt arbeidsforhold`() {
        arbeidsforholdRepository.save(lagArbeidsforhold(navArbeidsforholdId = "første", fnr = "første-ident"))

        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "første",
                        arbeidstakerIdenter = listOf("første-ident", "ny-ident"),
                    ),
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "andre",
                        arbeidstakerIdenter = listOf("første-ident", "ny-ident"),
                    ),
                ),
            ),
            fnr = "ny-ident",
        )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("ny-ident")

        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(listOf("første-ident", "ny-ident"))
        arbeidsforhold.size `should be equal to` 2

        arbeidsforhold.first().fnr `should be equal to` "første-ident"
        arbeidsforhold.last().fnr `should be equal to` "ny-ident"
    }

    @Test
    fun `burde oppdatere et arbeidsforhold selv om personen har endret ident`() {
        val originaltArbeidsforhold = lagArbeidsforhold(navArbeidsforholdId = "originaltArbeidsforhold", fnr = "første-ident")
        arbeidsforholdRepository.save(originaltArbeidsforhold)

        val oppdatertArbeidsforholdMedNyIdent =
            lagArbeidsforholdOversikt(
                navArbeidsforholdId = "originaltArbeidsforhold",
                arbeidstakerIdenter = listOf("første-ident", "ny-ident"),
            )
        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    oppdatertArbeidsforholdMedNyIdent,
                ),
            ),
            fnr = "ny-ident",
        )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("ny-ident")

        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(listOf("første-ident", "ny-ident"))
        arbeidsforhold.size `should be equal to` 1

        arbeidsforhold.first().fnr `should be equal to` "første-ident"
    }

    @Test
    fun `burde bruke identer fra PDL i tillegg til Aareg, fordi Aareg ikke er i sync i en migreringsperiode`() {
        arbeidsforholdRepository.save(
            lagArbeidsforhold(navArbeidsforholdId = "1", fnr = "aareg-ukjent-ident"),
        )

        pdlClient.setIdentMedHistorikk(
            listOf(
                PdlIdent(
                    gruppe = "FOLKEREGISTERIDENT",
                    ident = "kjent_ident",
                ),
                PdlIdent(
                    gruppe = "FOLKEREGISTERIDENT",
                    ident = "aareg-ukjent-ident",
                ),
            ),
            ident = "kjent_ident",
        )

        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "1",
                        arbeidstakerIdenter = listOf("kjent_ident"),
                    ),
                ),
            ),
            fnr = "kjent_ident",
        )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("kjent_ident")
        arbeidsforholdRepository.findAll().shouldHaveSize(1)
    }

    @Test
    fun `burde synkronisere fremtidig arbeidsforhold fra aareg`() {
        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "fremtidig",
                        arbeidstakerIdenter = listOf("fnr"),
                        startdato = LocalDate.parse("2024-12-01"),
                        sluttdato = null,
                    ),
                ),
            ),
            fnr = "fnr",
        )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("fnr")

        arbeidsforholdRepository
            .findByNavArbeidsforholdId("fremtidig")
            .shouldNotBeNull()
            .run {
                fnr shouldBeEqualTo "fnr"
                fom shouldBeEqualTo LocalDate.parse("2024-12-01")
                tom shouldBeEqualTo null
            }
    }
}
