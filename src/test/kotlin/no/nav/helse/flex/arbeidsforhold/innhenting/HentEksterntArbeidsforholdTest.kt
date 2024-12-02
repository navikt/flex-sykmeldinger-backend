package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.AaregClient
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.ArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.ArbeidsforholdoversiktResponse
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.Arbeidssted
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.Arbeidstaker
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.Ident
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.Kodeverksentitet
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.Opplysningspliktig
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HentEksterntArbeidsforholdTest {
    @Test
    fun `burde bruke arbeidsforholdInfo fra Aareg`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt()
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

    private fun lagArbeidsforholdOversikt(
        navArbeidsforholdId: String = "arbeidsforholdId",
        typeKode: String = "ordinaertArbeidsforhold",
        arbeidstakerIdent: Ident = Ident(
            type = "AKTORID",
            ident = "2175141353812",
            gjeldende = true
        ),
        arbeidsstedIdent: Ident = Ident(
            type = "ORGANISASJONSNUMMER",
            ident = "910825518",
        ),

        ): ArbeidsforholdOversikt =
        ArbeidsforholdOversikt(
            navArbeidsforholdId = navArbeidsforholdId,
            type = Kodeverksentitet(
                kode = typeKode,
                beskrivelse = "Ordinært arbeidsforhold",
            ),
            arbeidstaker = Arbeidstaker(
                identer = listOf(
                    arbeidstakerIdent
                )
            ),
            arbeidssted = Arbeidssted(
                type = "Underenhet",
                identer = listOf(
                    arbeidsstedIdent
                )
            ),
            opplysningspliktig = Opplysningspliktig(
                type = "Hovedenhet",
                identer = listOf(
                    Ident(
                        type = "ORGANISASJONSNUMMER",
                        ident = "810825472",
                    )
                )
            ),
            startdato = LocalDate.parse("2014-01-01"),
            sluttdato = LocalDate.parse("2014-01-01"),
            yrke = Kodeverksentitet(
                kode = "1231119",
                beskrivelse = "KONTORLEDER"
            ),
            avtaltStillingsprosent = 100,
            permisjonsprosent = 50,
            permitteringsprosent = 50
        )


}
