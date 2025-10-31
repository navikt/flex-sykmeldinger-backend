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
        resultat.opprett shouldHaveSize 1
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
        resultat.oppdater shouldHaveSize 1
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
        resultat.slett shouldHaveSize 1
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
        resultat.opprett shouldHaveSize 0
    }

    @Test
    fun `burde opprette arbeidsforhold som er fremtidig`() {
        val resultat =
            ArbeidsforholdInnhentingService.synkroniserArbeidsforhold(
                interneArbeidsforhold = emptyList(),
                eksterneArbeidsforhold =
                    listOf(
                        lagEksterntArbeidsforhold(
                            fom = LocalDate.parse("2024-12-01"),
                        ),
                    ),
                fnr = "fnr",
                now = Instant.parse("2024-05-01T00:00:00Z"),
            )
        resultat.opprett shouldHaveSize 1
    }

    @Test
    fun `burde oppdatere arbeidsforhold som er fremtidig`() {
        val resultat =
            ArbeidsforholdInnhentingService.synkroniserArbeidsforhold(
                interneArbeidsforhold =
                    listOf(
                        lagArbeidsforhold(
                            navArbeidsforholdId = "1",
                            fom = LocalDate.parse("2024-12-01"),
                        ),
                    ),
                eksterneArbeidsforhold =
                    listOf(
                        lagEksterntArbeidsforhold(
                            navArbeidsforholdId = "1",
                            fom = LocalDate.parse("2024-11-01"),
                        ),
                    ),
                fnr = "fnr",
                now = Instant.parse("2024-05-01T00:00:00Z"),
            )
        resultat.oppdater shouldHaveSize 1
    }

    @Test
    fun `burde slette fremtidig arbeidsforhold som ikke lenger finnes i aareg`() {
        val resultat =
            ArbeidsforholdInnhentingService.synkroniserArbeidsforhold(
                interneArbeidsforhold =
                    listOf(
                        lagArbeidsforhold(
                            navArbeidsforholdId = "1",
                            fom = LocalDate.parse("2024-12-01"),
                        ),
                    ),
                eksterneArbeidsforhold = emptyList(),
                fnr = "fnr",
                now = Instant.parse("2024-05-01T00:00:00Z"),
            )
        resultat.slett shouldHaveSize 1
    }
}
