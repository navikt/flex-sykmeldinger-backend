package no.nav.helse.flex.arbeidsforhold.innhenting.aareghendelser

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforhold
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test

class AaregHendelserListenerTest {
    @Test
    fun `burde synkronisere eksisterende persons arbeidsforhold`() {
        val arbeidsforholdRepository: ArbeidsforholdRepository =
            mock {
                on { getAllByFnr("fnr_med_sykmelding") } doReturn listOf(lagArbeidsforhold(fnr = "fnr_med_sykmelding"))
            }
        val listener = AaregHendelserListener(arbeidsforholdRepository)
        listener.skalSynkroniseres("fnr_med_sykmelding") `should be` true
    }

    @Test
    fun `burde ikke synkronisere ny persons arbeidsforhold`() {
        val arbeidsforholdRepository: ArbeidsforholdRepository =
            mock {
                on { getAllByFnr("fnr_uten_sykmelding") } doReturn emptyList()
            }
        val listener = AaregHendelserListener(arbeidsforholdRepository)
        listener.skalSynkroniseres("fnr_uten_sykmelding") `should be` false
    }
}
