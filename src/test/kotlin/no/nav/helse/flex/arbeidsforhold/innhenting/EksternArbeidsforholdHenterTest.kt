package no.nav.helse.flex.arbeidsforhold.innhenting

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.mock
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.EksternArbeidsforholdHenter.Companion.getOrgnummerFraArbeidssted
import no.nav.helse.flex.clients.aareg.*
import no.nav.helse.flex.clients.ereg.EregClient
import no.nav.helse.flex.clients.ereg.Navn
import no.nav.helse.flex.clients.ereg.Nokkelinfo
import no.nav.helse.flex.config.EnvironmentToggles
import org.amshove.kluent.*
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClientException
import java.time.LocalDate

class EksternArbeidsforholdHenterTest {
    fun eregClientMock(): EregClient =
        mock {
            on { hentNokkelinfo(any()) } doReturn Nokkelinfo(Navn("_"))
        }

    fun aaregClientMock(): AaregClient =
        mock {
            on { getArbeidstakerArbeidsforholdoversikt(any()) } doReturn ArbeidsforholdoversiktResponse(listOf(lagArbeidsforholdOversikt()))
        }

    fun environmentTogglesMock(): EnvironmentToggles =
        mock {
            on { isProduction() } doReturn true
        }

    @Test
    fun `burde bruke arbeidsforholdInfo fra Aareg`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidstakerArbeidsforholdoversikt("fnr") } doReturn
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
        val eksternArbeidsforholdHenter =
            EksternArbeidsforholdHenter(
                aaregClient = aaregClient,
                eregClient = eregClientMock(),
                environmentToggles = environmentTogglesMock(),
            )
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("fnr").eksterneArbeidsforhold.first()
        eksterntArbeidsforhold.navArbeidsforholdId `should be equal to` "navArbeidsforholdId"
        eksterntArbeidsforhold.arbeidsforholdType `should be equal to` ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
        eksterntArbeidsforhold.orgnummer `should be equal to` "orgnummer"
        eksterntArbeidsforhold.juridiskOrgnummer `should be equal to` "juridisk-orgnummer"
        eksterntArbeidsforhold.fom `should be equal to` LocalDate.parse("2020-01-01")
        eksterntArbeidsforhold.tom `should be equal to` LocalDate.parse("2020-01-02")
    }

    @Test
    fun `burde bruke tom liste når AAREG er nede i dev`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidstakerArbeidsforholdoversikt("fnr") } doThrow RestClientException("AAREG er nede")
            }
        val environmentToggles: EnvironmentToggles =
            mock {
                on { isProduction() } doReturn false
            }
        val eksternArbeidsforholdHenter =
            EksternArbeidsforholdHenter(
                aaregClient = aaregClient,
                eregClient = eregClientMock(),
                environmentToggles = environmentToggles,
            )
        eksternArbeidsforholdHenter
            .hentEksterneArbeidsforholdForPerson(
                "fnr",
            ).eksterneArbeidsforhold
            .`should be empty`()
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
                environmentToggles = environmentTogglesMock(),
            )
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").eksterneArbeidsforhold.first()
        eksterntArbeidsforhold.orgnavn `should be equal to` "Org Navn"
    }

    @Test
    fun `burde returnere riktig orgnummer`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidstakerArbeidsforholdoversikt(any()) } doReturn
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

        val eksternArbeidsforholdHenter =
            EksternArbeidsforholdHenter(
                aaregClient = aaregClient,
                eregClient = eregClientMock(),
                environmentToggles = environmentTogglesMock(),
            )
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").eksterneArbeidsforhold.first()
        eksterntArbeidsforhold.orgnummer `should be equal to` "orgnummer"
    }

    @Test
    fun `burde returnere riktig juridisk orgnummer`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidstakerArbeidsforholdoversikt(any()) } doReturn
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

        val eksternArbeidsforholdHenter =
            EksternArbeidsforholdHenter(
                aaregClient = aaregClient,
                eregClient = eregClientMock(),
                environmentToggles = environmentTogglesMock(),
            )
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").eksterneArbeidsforhold.first()
        eksterntArbeidsforhold.juridiskOrgnummer `should be equal to` "juridisk-orgnummer"
    }

    @Test
    fun `burde returnere riktig arbeidsforholdType`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidstakerArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                typeKode = "frilanserOppdragstakerHonorarPersonerMm",
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter =
            EksternArbeidsforholdHenter(
                aaregClient = aaregClient,
                eregClient = eregClientMock(),
                environmentToggles = environmentTogglesMock(),
            )
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").eksterneArbeidsforhold.first()
        eksterntArbeidsforhold.arbeidsforholdType `should be equal to` ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_HONORAR_PERSONER_MM
    }

    @Test
    fun `burde filtrere vekk arbeidsstedType som ikke er Underenhet`() {
        val aaregClient: AaregClient =
            mock {
                on { getArbeidstakerArbeidsforholdoversikt(any()) } doReturn
                    ArbeidsforholdoversiktResponse(
                        listOf(
                            lagArbeidsforholdOversikt(
                                arbeidsstedType = ArbeidsstedType.Person,
                            ),
                        ),
                    )
            }

        val eksternArbeidsforholdHenter =
            EksternArbeidsforholdHenter(
                aaregClient = aaregClient,
                eregClient = eregClientMock(),
                environmentToggles = environmentTogglesMock(),
            )
        eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_").eksterneArbeidsforhold shouldHaveSize 0
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
