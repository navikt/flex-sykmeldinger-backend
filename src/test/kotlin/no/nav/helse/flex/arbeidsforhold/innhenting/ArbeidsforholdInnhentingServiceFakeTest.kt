package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.*
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EregClientFake
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate

class ArbeidsforholdInnhentingServiceFakeTest : FakesTestOppsett() {
    @Autowired
    lateinit var arbeidsforholdInnhentingService: ArbeidsforholdInnhentingService

    @Autowired
    lateinit var aaregClientFake: AaregClientFake

    @Autowired
    lateinit var eregClientFake: EregClientFake

    @AfterEach
    fun tearDown() {
        aaregClientFake.reset()
        eregClientFake.reset()
    }

    @Test
    fun `lagrer arbeidsforhold fra eksternt arbeidsforhold som ikke finnes fra før`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
                    lagIdenterOgEksterneArbeidsforhold(
                        eksterneArbeidsforhold =
                            listOf(
                                lagEksterntArbeidsforhold(navArbeidsforholdId = "navArbeidsforholdId"),
                            ),
                    )
            }
        val arbeidsforholdRepository = mock<ArbeidsforholdRepository>()
        val arbeidsforholdInnhentingService =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("navArbeidsforholdId")
        verify(arbeidsforholdRepository).saveAll<Arbeidsforhold>(any())
    }

    @Test
    fun `lagrer oppdatert arbeidsforhold fra eksternt arbeidsforhold som finnes fra før`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
                    lagIdenterOgEksterneArbeidsforhold(
                        eksterneArbeidsforhold =
                            listOf(
                                lagEksterntArbeidsforhold(navArbeidsforholdId = "navArbeidsforholdId"),
                            ),
                    )
            }
        val arbeidsforholdRepository =
            mock<ArbeidsforholdRepository> {
                on { findByNavArbeidsforholdId(any()) } doReturn lagArbeidsforhold(navArbeidsforholdId = "navArbeidsforholdId")
            }
        val arbeidsforholdInnhentingService =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("navArbeidsforholdId")
        verify(arbeidsforholdRepository).saveAll<Arbeidsforhold>(any())
    }

    @Test
    fun `lagrer riktig data for nytt arbeidsforhold`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
                    lagIdenterOgEksterneArbeidsforhold(
                        eksterneArbeidsforhold =
                            listOf(
                                EksterntArbeidsforhold(
                                    navArbeidsforholdId = "arbeidsforhold",
                                    orgnummer = "orgnummer",
                                    juridiskOrgnummer = "jorgnummer",
                                    orgnavn = "Orgnavn",
                                    fom = LocalDate.parse("2020-01-01"),
                                    tom = null,
                                    arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
                                ),
                            ),
                    )
            }
        val arbeidsforholdRepository = mock<ArbeidsforholdRepository>()
        val arbeidsforholdInnhentingService =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
                nowFactory = { Instant.parse("2020-01-01T00:00:00Z") },
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(fnr = "fnr")

        val forventetArbeidsforhold =
            lagArbeidsforhold(
                navArbeidsforholdId = "arbeidsforhold",
                fnr = "fnr",
                orgnummer = "orgnummer",
                juridiskOrgnummer = "jorgnummer",
                orgnavn = "Orgnavn",
                fom = LocalDate.parse("2020-01-01"),
                tom = null,
                arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
                opprettet = Instant.parse("2020-01-01T00:00:00Z"),
            )
        verify(arbeidsforholdRepository).saveAll(listOf(forventetArbeidsforhold))
    }

    @Test
    fun `lagrer riktig data for oppdatert arbeidsforhold`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
                    lagIdenterOgEksterneArbeidsforhold(
                        identer = PersonIdenter(originalIdent = "nytt_fnr"),
                        eksterneArbeidsforhold =
                            listOf(
                                EksterntArbeidsforhold(
                                    navArbeidsforholdId = "arbeidsforhold",
                                    orgnummer = "nytt_orgnummer",
                                    juridiskOrgnummer = "nytt_jorgnummer",
                                    orgnavn = "nytt_Orgnavn",
                                    fom = LocalDate.parse("2020-01-01"),
                                    tom = null,
                                    arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
                                ),
                            ),
                    )
            }
        val arbeidsforholdRepository =
            mock<ArbeidsforholdRepository> {
                on { findByNavArbeidsforholdId(any()) } doReturn
                    lagArbeidsforhold(
                        navArbeidsforholdId = "arbeidsforhold",
                        fnr = "fnr",
                        orgnummer = "orgnummer",
                        juridiskOrgnummer = "jorgnummer",
                        orgnavn = "Orgnavn",
                        fom = LocalDate.parse("2020-01-01"),
                        tom = null,
                        arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
                        opprettet = Instant.parse("2020-01-01T00:00:00Z"),
                    )
            }
        val arbeidsforholdInnhentingService =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
                nowFactory = { Instant.parse("2020-01-01T00:00:00Z") },
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson(fnr = "nytt_fnr")

        val forventetArbeidsforhold =
            lagArbeidsforhold(
                navArbeidsforholdId = "arbeidsforhold",
                fnr = "nytt_fnr",
                orgnummer = "nytt_orgnummer",
                juridiskOrgnummer = "nytt_jorgnummer",
                orgnavn = "nytt_Orgnavn",
                fom = LocalDate.parse("2020-01-01"),
                tom = null,
                arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
                opprettet = Instant.parse("2020-01-01T00:00:00Z"),
            )
        verify(arbeidsforholdRepository).saveAll(listOf(forventetArbeidsforhold))
    }

    @Test
    fun `burde opprette et arbeidsforhold når personen har endret ident og fått nytt arbeidsforhold`() {
        arbeidsforholdRepository.save(lagArbeidsforhold(navArbeidsforholdId = "første", fnr = "første-ident"))

        aaregClientFake.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "første",
                        identer = listOf("første-ident", "ny-ident"),
                    ),
                    lagArbeidsforholdOversikt(
                        navArbeidsforholdId = "andre",
                        identer = listOf("første-ident", "ny-ident"),
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
                identer = listOf("første-ident", "ny-ident"),
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
}
