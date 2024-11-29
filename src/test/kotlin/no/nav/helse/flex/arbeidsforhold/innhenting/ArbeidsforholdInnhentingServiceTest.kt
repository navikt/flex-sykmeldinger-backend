package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import org.junit.jupiter.api.Test

class ArbeidsforholdInnhentingServiceTest {
    @Test
    fun `burde synkronisere et arbeidsforhold`() {
        val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter =
            mock {
                on { hentEksterntArbeidsforhold(any()) } doReturn EksterntArbeidsforhold()
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
}
