package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import org.amshove.kluent.`should be`
import org.junit.jupiter.api.Test

class ArbeidsforholdInnhentingRuterServiceTest {
    @Test
    fun `burde synkronisere eksisterende persons arbeidsforhold`() {
        val registrertePersonerForArbeidsforhold: RegistrertePersonerForArbeidsforhold =
            mock {
                on { erPersonRegistrert("fnr_med_sykmelding") } doReturn true
            }
        val service = ArbeidsforholdInnhentingRuterService(registrertePersonerForArbeidsforhold, mock())
        service.skalSynkroniseres("fnr_med_sykmelding") `should be` true
    }

    @Test
    fun `burde ikke synkronisere ny persons arbeidsforhold`() {
        val registrertePersonerForArbeidsforhold: RegistrertePersonerForArbeidsforhold =
            mock {
                on { erPersonRegistrert("fnr_uten_sykmelding") } doReturn false
            }
        val service = ArbeidsforholdInnhentingRuterService(registrertePersonerForArbeidsforhold, mock())
        service.skalSynkroniseres("fnr_uten_sykmelding") `should be` false
    }
}
