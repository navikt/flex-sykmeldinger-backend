package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ArbeidsforholdInnhentingServiceTest {
    @Test
    fun `lagrer arbeidsforhold fra eksternt arbeidsforhold som ikke finnes fra før`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
                    listOf(
                        lagEksterntArbeidsforhold(arbeidsforholdId = "arbeidsforholdId"),
                    )
            }
        val arbeidsforholdRepository = mock<ArbeidsforholdRepository>()
        val arbeidsforholdInnhentingService =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("arbeidsforholdId")
        verify(arbeidsforholdRepository).saveAll<Arbeidsforhold>(any())
    }

    @Test
    fun `lagrer oppdatert arbeidsforhold fra eksternt arbeidsforhold som finnes fra før`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
                    listOf(
                        lagEksterntArbeidsforhold(arbeidsforholdId = "arbeidsforholdId"),
                    )
            }
        val arbeidsforholdRepository =
            mock<ArbeidsforholdRepository> {
                on { findByArbeidsforholdId(any()) } doReturn lagArbeidsforhold(arbeidsforholdId = "arbeidsforholdId")
            }
        val arbeidsforholdInnhentingService =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("arbeidsforholdId")
        verify(arbeidsforholdRepository).saveAll<Arbeidsforhold>(any())
    }

    @Test
    fun `lagrer riktig data for nytt arbeidsforhold`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
                    listOf(
                        EksterntArbeidsforhold(
                            arbeidsforholdId = "arbeidsforhold",
                            fnr = "fnr",
                            orgnummer = "orgnummer",
                            juridiskOrgnummer = "jorgnummer",
                            orgnavn = "Orgnavn",
                            fom = LocalDate.parse("2020-01-01"),
                            tom = null,
                            arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
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

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("arbeidsforhold")

        val forventetArbeidsforhold =
            lagArbeidsforhold(
                arbeidsforholdId = "arbeidsforhold",
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
                    listOf(
                        EksterntArbeidsforhold(
                            arbeidsforholdId = "arbeidsforhold",
                            fnr = "nytt_fnr",
                            orgnummer = "nytt_orgnummer",
                            juridiskOrgnummer = "nytt_jorgnummer",
                            orgnavn = "nytt_Orgnavn",
                            fom = LocalDate.parse("2020-01-01"),
                            tom = null,
                            arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
                        ),
                    )
            }
        val arbeidsforholdRepository =
            mock<ArbeidsforholdRepository> {
                on { findByArbeidsforholdId(any()) } doReturn
                    lagArbeidsforhold(
                        arbeidsforholdId = "arbeidsforhold",
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

        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("arbeidsforhold")

        val forventetArbeidsforhold =
            lagArbeidsforhold(
                arbeidsforholdId = "arbeidsforhold",
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
    fun `burde opprette nye arbeidsforhold i aareg`() {
        val service =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = mock<EksternArbeidsforholdHenter>(),
                arbeidsforholdRepository = mock<ArbeidsforholdRepository>(),
            )

        val resultat =
            service.synkroniserArbeidsforhold(
                interneArbeidsforhold = emptyList(),
                eksterneArbeidsforhold =
                    listOf(
                        lagEksterntArbeidsforhold(),
                    ),
            )
        resultat.skalOpprettes shouldHaveSize 1
    }

    @Test
    fun `burde oppdatere eksisterende arbeidsforhold i aareg`() {
        val service =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = mock<EksternArbeidsforholdHenter>(),
                arbeidsforholdRepository = mock<ArbeidsforholdRepository>(),
            )

        val resultat =
            service.synkroniserArbeidsforhold(
                interneArbeidsforhold =
                    listOf(
                        lagArbeidsforhold(arbeidsforholdId = "1"),
                    ),
                eksterneArbeidsforhold =
                    listOf(
                        lagEksterntArbeidsforhold(arbeidsforholdId = "1"),
                    ),
            )
        resultat.skalOppdateres shouldHaveSize 1
    }

    @Test
    fun `burde slette arbeidsforhold som ikke finnes i aareg lengre`() {
        val service =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = mock<EksternArbeidsforholdHenter>(),
                arbeidsforholdRepository = mock<ArbeidsforholdRepository>(),
            )

        val resultat =
            service.synkroniserArbeidsforhold(
                interneArbeidsforhold =
                    listOf(
                        lagArbeidsforhold(id = ""),
                    ),
                eksterneArbeidsforhold = emptyList(),
            )
        resultat.skalSlettes shouldHaveSize 1
    }

    @Test
    fun `burde ikke opprette arbeidsforhold der man ikke har vært ansatt de siste 4 mnd`() {
        val service =
            ArbeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = mock<EksternArbeidsforholdHenter>(),
                arbeidsforholdRepository = mock<ArbeidsforholdRepository>(),
                nowFactory = { Instant.parse("2020-05-01T00:00:00Z") },
            )

        val resultat =
            service.synkroniserArbeidsforhold(
                interneArbeidsforhold = emptyList(),
                eksterneArbeidsforhold =
                    listOf(
                        lagEksterntArbeidsforhold(
                            tom = LocalDate.parse("2020-01-01"),
                        ),
                    ),
            )
        resultat.skalOpprettes shouldHaveSize 0
    }
}
