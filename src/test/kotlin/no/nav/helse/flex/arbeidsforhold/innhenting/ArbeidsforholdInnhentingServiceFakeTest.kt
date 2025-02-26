package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.*
import no.nav.helse.flex.clients.pdl.PersonIdenter
import no.nav.helse.flex.testconfig.FakesTestOppsett
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ArbeidsforholdInnhentingServiceFakeTest : FakesTestOppsett() {
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
}
