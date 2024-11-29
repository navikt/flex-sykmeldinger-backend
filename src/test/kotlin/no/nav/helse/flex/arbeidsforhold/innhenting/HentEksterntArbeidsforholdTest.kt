package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.AaregClient
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.ArbeidsforholdoversiktResponse
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class HentEksterntArbeidsforholdTest {
    @Test
    fun `burde bruke arbeidsforholdInfo fra Aareg`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            // TODO
                        ),
                    )
            }
        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient)
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterntArbeidsforhold("arbeidsforhold")
        eksterntArbeidsforhold.arbeidsforholdId `should be equal to` "arbeidsforhold"
        eksterntArbeidsforhold.arbeidsforholdType `should be equal to` ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
        eksterntArbeidsforhold.orgnummer `should be equal to` "arbeidsforhold.orgnummer"
        eksterntArbeidsforhold.juridiskOrgnummer `should be equal to` "arbeidsforhold.orgnummer"
        eksterntArbeidsforhold.fnr `should be equal to` "arbeidsforhold.fnr"
        eksterntArbeidsforhold.fom `should be equal to` "arbeidsforhold.fom"
        eksterntArbeidsforhold.tom `should be equal to` "tom"
    }
}
