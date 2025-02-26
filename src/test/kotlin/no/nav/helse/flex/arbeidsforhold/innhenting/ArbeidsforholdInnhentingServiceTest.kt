package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.arbeidsforhold.lagEksterntArbeidsforhold
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ArbeidsforholdInnhentingServiceTest {
    @Test
    fun `burde opprette nye arbeidsforhold i aareg`() {
        val resultat =
            ArbeidsforholdInnhentingService.synkroniserArbeidsforhold(
                interneArbeidsforhold = emptyList(),
                eksterneArbeidsforhold =
                    listOf(
                        lagEksterntArbeidsforhold(),
                    ),
                fnr = "fnr",
            )
        resultat.skalOpprettes shouldHaveSize 1
    }

    @Test
    fun `burde oppdatere eksisterende arbeidsforhold i aareg`() {
        val resultat =
            ArbeidsforholdInnhentingService.synkroniserArbeidsforhold(
                interneArbeidsforhold =
                    listOf(
                        lagArbeidsforhold(navArbeidsforholdId = "1"),
                    ),
                eksterneArbeidsforhold =
                    listOf(
                        lagEksterntArbeidsforhold(navArbeidsforholdId = "1"),
                    ),
                fnr = "fnr",
            )
        resultat.skalOppdateres shouldHaveSize 1
    }

    @Test
    fun `burde slette arbeidsforhold som ikke finnes i aareg lengre`() {
        val resultat =
            ArbeidsforholdInnhentingService.synkroniserArbeidsforhold(
                interneArbeidsforhold =
                    listOf(
                        lagArbeidsforhold(id = ""),
                    ),
                eksterneArbeidsforhold = emptyList(),
                fnr = "fnr",
            )
        resultat.skalSlettes shouldHaveSize 1
    }

    @Test
    fun `burde ikke opprette arbeidsforhold der man ikke har vært ansatt de siste 4 mnd`() {
        val resultat =
            ArbeidsforholdInnhentingService.synkroniserArbeidsforhold(
                interneArbeidsforhold = emptyList(),
                eksterneArbeidsforhold =
                    listOf(
                        lagEksterntArbeidsforhold(
                            tom = LocalDate.parse("2020-01-01"),
                        ),
                    ),
                fnr = "fnr",
                now = Instant.parse("2020-05-01T00:00:00Z"),
            )
        resultat.skalOpprettes shouldHaveSize 0
    }

//    @Test
//    fun `burde hente arbeidsforhold for flere identer`() {
//        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
//            mock {
//                on { hentEksterneArbeidsforholdForPerson(any()) } doReturn
//                    listOf(
//                        lagEksterntArbeidsforhold(navArbeidsforholdId = "navArbeidsforholdId1"),
//                        lagEksterntArbeidsforhold(navArbeidsforholdId = "navArbeidsforholdId2"),
//                    )
//            }
//        val arbeidsforholdRepository = mock<ArbeidsforholdRepository>()
//        val arbeidsforholdInnhentingService =
//            ArbeidsforholdInnhentingService(
//                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
//                arbeidsforholdRepository = arbeidsforholdRepository,
//            )
//
//        arbeidsforholdInnhentingService.synkroniserArbeidsforholdForPerson("navArbeidsforholdId1", "navArbeidsforholdId2")
//        verify(arbeidsforholdRepository).saveAll<Arbeidsforhold>(any())
//    }
}
