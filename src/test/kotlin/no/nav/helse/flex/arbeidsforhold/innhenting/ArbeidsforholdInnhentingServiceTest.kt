package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import org.junit.jupiter.api.Test

class ArbeidsforholdInnhentingServiceTest {
    @Test
    fun `oppretter arbeidsforhold fra eksternt arbeidsforhold som ikke finnes fra før`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterntArbeidsforhold(any()) } doReturn EksterntArbeidsforhold("arbeidsforhold")
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
    fun `synkroniserer arbeidsforhold fra eksternt arbeidsforhold som finnes fra før`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterntArbeidsforhold(any()) } doReturn EksterntArbeidsforhold(
                    arbeidsforholdId = "arbeidsforholdId",
                )
            }
        val arbeidsforholdRepository = mock<ArbeidsforholdRepository> {
            on { findByArbeidsforholdId(any()) } doReturn lagArbeidsforhold(arbeidsforholdId = "arbeidsforholdId")
        }
        val arbeidsforholdInnhentingService =
            ArebeidsforholdInnhentingService(
                eksternArbeidsforholdHenter = eksternArbeidsforholdHenter,
                arbeidsforholdRepository = arbeidsforholdRepository,
            )

        arbeidsforholdInnhentingService.synkroniserArbeidsforhold("arbeidsforholdId")
        verify(arbeidsforholdRepository).save(eq(lagArbeidsforhold(arbeidsforholdId = "arbeidsforholdId")))
    }
}
