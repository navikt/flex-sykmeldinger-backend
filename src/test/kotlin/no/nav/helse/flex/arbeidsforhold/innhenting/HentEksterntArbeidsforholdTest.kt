package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.*
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HentEksterntArbeidsforholdTest {
    @Test
    fun `burde bruke arbeidsforholdInfo fra Aareg`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt("fnr") } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                navArbeidsforholdId = "arbeidsforholdId",
                                typeKode = "ordinaertArbeidsforhold",
                                arbeidstakerIdent =
                                    Ident(
                                        type = "AKTORID",
                                        ident = "00000000001",
                                        gjeldende = true,
                                    ),
                                arbeidsstedIdent =
                                    Ident(
                                        type = "ORGANISASJONSNUMMER",
                                        ident = "orgnummer",
                                    ),
                                opplysningspliktig =
                                    Ident(
                                        type = "ORGANISASJONSNUMMER",
                                        ident = "juridisk-orgnummer",
                                    ),
                                startdato = LocalDate.parse("2020-01-01"),
                                sluttdato = LocalDate.parse("2020-01-02"),
                            ),
                        ),
                    )
            }
        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient)
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("fnr").first()
        eksterntArbeidsforhold.arbeidsforholdId `should be equal to` "arbeidsforholdId"
        eksterntArbeidsforhold.arbeidsforholdType `should be equal to` ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
        eksterntArbeidsforhold.orgnummer `should be equal to` "orgnummer"
        eksterntArbeidsforhold.juridiskOrgnummer `should be equal to` "juridisk-orgnummer"
        eksterntArbeidsforhold.fnr `should be equal to` "00000000001"
        eksterntArbeidsforhold.fom `should be equal to` LocalDate.parse("2020-01-01")
        eksterntArbeidsforhold.tom `should be equal to` LocalDate.parse("2020-01-02")
    }

    @Test
    fun `finner orgnummer i arbeidssted med én ident`() {
        val arbeidssted = Arbeidssted(
            type=ArbeidsstedType.Underenhet,
            identer = listOf(
                Ident(
                    type="ORGANISASJONSNUMMER",
                    ident = "orgnummer",
                )
            )
        )
        getOrgnummerFraArbeidssted(arbeidssted) `should be equal to` "orgnummer"
    }

    @Test
    fun `finner ikke orgnummer i arbeidssted dersom feil ident type`() {
        val arbeidssted = Arbeidssted(
            type=ArbeidsstedType.Underenhet,
            identer = listOf(
                Ident(
                    type="_",
                    ident = "orgnummer",
                )
            )
        )
        invoking {
            getOrgnummerFraArbeidssted(arbeidssted) `should be equal to` "orgnummer"
        } shouldThrow(Exception::class)
    }

    private fun lagArbeidsforholdOversikt(
        navArbeidsforholdId: String = "arbeidsforholdId",
        typeKode: String = "ordinaertArbeidsforhold",
        arbeidstakerIdent: Ident =
            Ident(
                type = "AKTORID",
                ident = "2175141353812",
                gjeldende = true,
            ),
        arbeidsstedIdent: Ident =
            Ident(
                type = "ORGANISASJONSNUMMER",
                ident = "910825518",
            ),
        opplysningspliktig: Ident =
            Ident(
                type = "ORGANISASJONSNUMMER",
                ident = "810825472",
            ),
        startdato: LocalDate = LocalDate.parse("2020-01-01"),
        sluttdato: LocalDate = LocalDate.parse("2020-01-01"),
    ): ArbeidsforholdOversikt {
        return ArbeidsforholdOversikt(
            navArbeidsforholdId = navArbeidsforholdId,
            type =
                Kodeverksentitet(
                    kode = typeKode,
                    beskrivelse = "Ordinært arbeidsforhold",
                ),
            arbeidstaker =
                Arbeidstaker(
                    identer =
                        listOf(
                            arbeidstakerIdent,
                        ),
                ),
            arbeidssted =
                Arbeidssted(
                    type = ArbeidsstedType.Underenhet,
                    identer =
                        listOf(
                            arbeidsstedIdent,
                        ),
                ),
            opplysningspliktig =
                Opplysningspliktig(
                    type = "Hovedenhet",
                    identer =
                        listOf(
                            opplysningspliktig,
                        ),
                ),
            startdato = startdato,
            sluttdato = sluttdato,
            yrke =
                Kodeverksentitet(
                    kode = "1231119",
                    beskrivelse = "KONTORLEDER",
                ),
            avtaltStillingsprosent = 100,
            permisjonsprosent = 50,
            permitteringsprosent = 50,
        )
    }
}
