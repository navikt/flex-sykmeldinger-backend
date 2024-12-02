package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import org.amshove.kluent.`should be equal to`
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
            ArebeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforhold("arbeidsforholdId")
        verify(arbeidsforholdRepository).save(any())
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
            ArebeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforhold("arbeidsforholdId")
        verify(arbeidsforholdRepository).save(any())
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
            ArebeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
                nowFactory = { Instant.parse("2020-01-01T00:00:00Z") },
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforhold("arbeidsforhold")

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
        verify(arbeidsforholdRepository).save(forventetArbeidsforhold)
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
            ArebeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
                nowFactory = { Instant.parse("2020-01-01T00:00:00Z") },
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforhold("arbeidsforhold")

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
        verify(arbeidsforholdRepository).save(forventetArbeidsforhold)
    }

    @Test
    fun `burde ignorere arbeidsforhold der man ikke har vært ansatt siste 4 mnd`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
                    listOf(
                        lagEksterntArbeidsforhold(tom = LocalDate.parse("2020-01-01")),
                    )
            }
        val arbeidsforholdInnhentingService =
            ArebeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = mock<ArbeidsforholdRepository>(),
                nowFactory = { Instant.parse("2020-05-01T00:00:00Z") },
            )

        val resultat = arbeidsforholdInnhentingService.synkroniserArbeidsforhold("_")
        resultat.antallIgnorerte `should be equal to` 1
    }
}
