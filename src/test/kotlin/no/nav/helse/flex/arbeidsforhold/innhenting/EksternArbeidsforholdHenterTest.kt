package no.nav.helse.flex.arbeidsforhold.innhenting

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.gateways.aareg.*
import no.nav.helse.flex.gateways.ereg.HentOrganisasjonerResponse
import no.nav.helse.flex.gateways.ereg.Navn
import no.nav.helse.flex.gateways.ereg.OrganisasjonInfo
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testconfig.fakes.EregClientFake
import no.nav.helse.flex.testutils.LoggLytter
import no.nav.helse.flex.utils.logger
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClientException
import java.time.LocalDate

class EksternArbeidsforholdHenterTest : FakesTestOppsett() {
    @Autowired
    lateinit var aaregClient: AaregClientFake

    @Autowired
    lateinit var eregClient: EregClientFake

    @Autowired
    lateinit var environmentToggles: EnvironmentTogglesFake

    @Autowired
    lateinit var eksternArbeidsforholdHenter: EksternArbeidsforholdHenter

    @AfterEach
    fun afterEach() {
        aaregClient.reset()
        eregClient.reset()
        environmentToggles.reset()
    }

    @Test
    fun `burde bruke arbeidsforholdInfo fra Aareg`() {
        aaregClient.setArbeidsforholdoversikt(
            fnr = "fnr",
            arbeidsforhold =
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
                ),
        )

        val resultat = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("fnr")
        resultat.eksterneArbeidsforhold.shouldBeSingle().run {
            navArbeidsforholdId shouldBe "navArbeidsforholdId"
            arbeidsforholdType shouldBe ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
            orgnummer shouldBe "orgnummer"
            juridiskOrgnummer shouldBe "juridisk-orgnummer"
            fom shouldBe LocalDate.parse("2020-01-01")
            tom shouldBe LocalDate.parse("2020-01-02")
        }
    }

    @Test
    fun `burde bruke tom liste når AAREG er nede i dev`() {
        aaregClient.setArbeidsforholdoversikt(
            fnr = "fnr",
            failure = RestClientException("AAREG er nede"),
        )
        environmentToggles.setEnvironment("dev")

        val resultat = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("fnr")
        resultat.eksterneArbeidsforhold.shouldBeEmpty()
    }

    @Test
    fun `burde logge melding ved aareg timeout`() {
        aaregClient.setArbeidsforholdoversikt(failure = ResourceAccessException("feilmelding"))
        val loggLytter = LoggLytter(eksternArbeidsforholdHenter.logger())

        shouldThrow<ResourceAccessException> {
            eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_")
        }

        loggLytter.logEventer().shouldBeSingle().run {
            message shouldEndWith "feilmelding"
        }
    }

    @Test
    fun `burde hente org navn fra ereg`() {
        aaregClient.setArbeidsforholdoversikt(
            arbeidsforhold = lagArbeidsforholdOversiktResponse(),
        )
        eregClient.setHentOrganisasjonerResponse(
            HentOrganisasjonerResponse(
                organisasjoner = mapOf("910825518" to OrganisasjonInfo(Navn("Testbedriften AS"))),
            ),
        )

        val resultat = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_")
        resultat.eksterneArbeidsforhold.shouldBeSingle().run {
            orgnavn shouldBe "Testbedriften AS"
        }
    }

    @Test
    fun `burde returnere riktig orgnummer`() {
        aaregClient.setArbeidsforholdoversikt(
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
            ),
        )
        val result = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_")
        result.eksterneArbeidsforhold.shouldBeSingle().run {
            orgnummer shouldBe "orgnummer"
        }
    }

    @Test
    fun `burde returnere riktig juridisk orgnummer`() {
        aaregClient.setArbeidsforholdoversikt(
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
            ),
        )
        val resultat = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_")
        resultat.eksterneArbeidsforhold.shouldBeSingle().run {
            juridiskOrgnummer shouldBe "juridisk-orgnummer"
        }
    }

    @Test
    fun `burde returnere riktig arbeidsforholdType`() {
        aaregClient.setArbeidsforholdoversikt(
            ArbeidsforholdoversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        typeKode = "frilanserOppdragstakerHonorarPersonerMm",
                    ),
                ),
            ),
        )
        val result = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_")
        result.eksterneArbeidsforhold.shouldBeSingle().run {
            arbeidsforholdType shouldBe ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_HONORAR_PERSONER_MM
        }
    }

    @Test
    fun `burde filtrere vekk arbeidsstedType som ikke er Underenhet`() {
        aaregClient.setArbeidsforholdoversikt(
            ArbeidsforholdoversiktResponse(
                listOf(
                    lagArbeidsforholdOversikt(
                        arbeidsstedType = ArbeidsstedType.Person,
                    ),
                ),
            ),
        )

        val resultat = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson("_")
        resultat.eksterneArbeidsforhold shouldHaveSize 0
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
        )
}
