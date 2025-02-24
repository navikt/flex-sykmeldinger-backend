package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.EksternArbeidsforholdHenter.Companion.getOrgnummerFraArbeidssted
import no.nav.helse.flex.clients.aareg.*
import no.nav.helse.flex.clients.ereg.EregClient
import no.nav.helse.flex.clients.ereg.Navn
import no.nav.helse.flex.clients.ereg.Nokkelinfo
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class EksternArbeidsforholdHenterTest {
    fun eregClientMock(): EregClient =
        mock {
            on { hentNokkelinfo(any()) } doReturn Nokkelinfo(Navn("_"))
        }

    fun aaregClientMock(): AaregClient =
        mock {
            on { getArbeidsforholdoversikt(any()) } doReturn ArbeidsforholdoversiktResponse(listOf(lagArbeidsforholdOversikt()))
        }

    @Test
    fun `burde bruke arbeidsforholdInfo fra Aareg`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt("fnr") } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                navArbeidsforholdId = "navArbeidsforholdId",
                                typeKode = "ordinaertArbeidsforhold",
                                arbeidstakerIdenter =
                                    listOf(
                                        Ident(
                                            type = IdentType.FOLKEREGISTERIDENT,
                                            ident = "00000000001",
                                            gjeldende = true,
                                        ),
                                    ),
                                arbeidsstedIdent =
                                    Ident(
                                        type = IdentType.ORGANISASJONSNUMMER,
                                        ident = "orgnummer",
                                    ),
                                opplysningspliktig =
                                    Ident(
                                        type = IdentType.ORGANISASJONSNUMMER,
                                        ident = "juridisk-orgnummer",
                                    ),
                                startdato = LocalDate.parse("2020-01-01"),
                                sluttdato = LocalDate.parse("2020-01-02"),
                            ),
                        ),
                    )
            }
        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient, eregClient = eregClientMock())
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("fnr").first()
        eksterntArbeidsforhold.navArbeidsforholdId `should be equal to` "navArbeidsforholdId"
        eksterntArbeidsforhold.arbeidsforholdType `should be equal to` ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
        eksterntArbeidsforhold.orgnummer `should be equal to` "orgnummer"
        eksterntArbeidsforhold.juridiskOrgnummer `should be equal to` "juridisk-orgnummer"
        eksterntArbeidsforhold.fnr `should be equal to` "00000000001"
        eksterntArbeidsforhold.fom `should be equal to` LocalDate.parse("2020-01-01")
        eksterntArbeidsforhold.tom `should be equal to` LocalDate.parse("2020-01-02")
    }

    @Test
    fun `burde hente org navn fra ereg`() {
        val eregClient: EregClient =
            mock {
                on { hentNokkelinfo(any()) } doReturn Nokkelinfo(Navn("Org Navn"))
            }

        val eksternArbeidsforholdHenter =
            EksternArbeidsforholdHenter(
                aaregClient = aaregClientMock(),
                eregClient = eregClient,
            )
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").first()
        eksterntArbeidsforhold.orgnavn `should be equal to` "Org Navn"
    }

    @ParameterizedTest
    @ValueSource(strings = ["FOLKEREGISTERIDENT"])
    fun `burde godta riktige ident typer for fnr`(identType: String) {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                arbeidstakerIdenter =
                                    listOf(
                                        Ident(
                                            type = IdentType.valueOf(identType),
                                            ident = "00000000001",
                                            gjeldende = true,
                                        ),
                                    ),
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient, eregClient = eregClientMock())
        val resultat = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").first()
        resultat.fnr `should be equal to` "00000000001"
    }

    @ParameterizedTest
    @ValueSource(strings = ["AKTORID", "ORGANISASJONSNUMMER"])
    fun `burde feile ved feil fnr ident type`(identType: String) {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                arbeidstakerIdenter =
                                    listOf(
                                        Ident(
                                            type = IdentType.valueOf(identType),
                                            ident = "00000000001",
                                            gjeldende = true,
                                        ),
                                    ),
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient, eregClient = eregClientMock())
        invoking {
            eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").first()
        } `should throw` Exception::class
    }

    @Test
    fun `bruker gjeldende fnr ident for arbeidstaker`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                arbeidstakerIdenter =
                                    listOf(
                                        Ident(
                                            type = IdentType.FOLKEREGISTERIDENT,
                                            ident = "gjeldende-fnr",
                                            gjeldende = true,
                                        ),
                                        Ident(
                                            type = IdentType.FOLKEREGISTERIDENT,
                                            ident = "ikke-gjeldende-fnr",
                                            gjeldende = false,
                                        ),
                                    ),
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient, eregClient = eregClientMock())

        val resultat = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").first()
        resultat.fnr `should be equal to` "gjeldende-fnr"
    }

    @Test
    fun `burde returnere riktig orgnummer`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                arbeidsstedIdent =
                                    Ident(
                                        type = IdentType.ORGANISASJONSNUMMER,
                                        ident = "orgnummer",
                                    ),
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient, eregClient = eregClientMock())
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").first()
        eksterntArbeidsforhold.orgnummer `should be equal to` "orgnummer"
    }

    @Test
    fun `burde returnere riktig juridisk orgnummer`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                opplysningspliktig =
                                    Ident(
                                        type = IdentType.ORGANISASJONSNUMMER,
                                        ident = "juridisk-orgnummer",
                                    ),
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient, eregClient = eregClientMock())
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").first()
        eksterntArbeidsforhold.juridiskOrgnummer `should be equal to` "juridisk-orgnummer"
    }

    @Test
    fun `burde returnere riktig arbeidsforholdType`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                typeKode = "frilanserOppdragstakerHonorarPersonerMm",
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient, eregClient = eregClientMock())
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").first()
        eksterntArbeidsforhold.arbeidsforholdType `should be equal to` ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_HONORAR_PERSONER_MM
    }

    @Test
    fun `burde filtrere vekk arbeidsstedType som ikke er Underenhet`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                arbeidsstedType = ArbeidsstedType.Person,
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter = EksternArbeidsforholdHenter(aaregClient = aaregClient, eregClient = eregClientMock())
        eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_") shouldHaveSize 0
    }

    @Test
    fun `getOrgnummerFraArbeidssted finner orgnummer i arbeidssted med én ident`() {
        val arbeidssted =
            Arbeidssted(
                type = ArbeidsstedType.Underenhet,
                identer =
                    listOf(
                        Ident(
                            type = IdentType.ORGANISASJONSNUMMER,
                            ident = "orgnummer",
                        ),
                    ),
            )
        getOrgnummerFraArbeidssted(arbeidssted) `should be equal to` "orgnummer"
    }

    @Test
    fun `getOrgnummerFraArbeidssted finner ikke orgnummer i arbeidssted dersom feil ident type`() {
        val arbeidssted =
            Arbeidssted(
                type = ArbeidsstedType.Underenhet,
                identer =
                    listOf(
                        Ident(
                            type = IdentType.FOLKEREGISTERIDENT,
                            ident = "orgnummer",
                        ),
                    ),
            )
        invoking {
            getOrgnummerFraArbeidssted(arbeidssted) `should be equal to` "orgnummer"
        } shouldThrow (Exception::class)
    }

    private fun lagArbeidsforholdOversikt(
        navArbeidsforholdId: String = "navArbeidsforholdId",
        typeKode: String = "ordinaertArbeidsforhold",
        arbeidstakerIdenter: List<Ident> =
            listOf(
                Ident(
                    type = IdentType.FOLKEREGISTERIDENT,
                    ident = "2175141353812",
                    gjeldende = true,
                ),
            ),
        arbeidsstedIdent: Ident =
            Ident(
                type = IdentType.ORGANISASJONSNUMMER,
                ident = "910825518",
            ),
        arbeidsstedType: ArbeidsstedType = ArbeidsstedType.Underenhet,
        opplysningspliktig: Ident =
            Ident(
                type = IdentType.ORGANISASJONSNUMMER,
                ident = "810825472",
            ),
        startdato: LocalDate = LocalDate.parse("2020-01-01"),
        sluttdato: LocalDate = LocalDate.parse("2020-01-01"),
    ): ArbeidsforholdOversikt =
        ArbeidsforholdOversikt(
            navArbeidsforholdId = navArbeidsforholdId,
            type =
                Kodeverksentitet(
                    kode = typeKode,
                    beskrivelse = "Ordinært arbeidsforhold",
                ),
            arbeidstaker =
                Arbeidstaker(
                    identer = arbeidstakerIdenter,
                ),
            arbeidssted =
                Arbeidssted(
                    type = arbeidsstedType,
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
