package no.nav.helse.flex.gateways

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import no.nav.helse.flex.gateways.ereg.EregClient
import no.nav.helse.flex.gateways.ereg.EregEksternClient
import no.nav.helse.flex.gateways.ereg.HentOrganisasjonerRequest
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultEregDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClientException
import tools.jackson.databind.JsonNode

@RestClientOppsett
@Import(EregEksternClient::class)
class EregClientTest {
    @Autowired
    private lateinit var eregMockWebServer: MockWebServer

    @Autowired
    private lateinit var eregEksternClient: EregClient

    @AfterEach
    fun afterEach() {
        eregMockWebServer.dispatcher = defaultEregDispatcher
    }

    @Test
    fun `burde kalle med riktig path som inkluderer orgnr`() {
        var path: String? = null
        eregMockWebServer.dispatcher =
            simpleDispatcher { request ->
                path = request.url.encodedPath
                MockResponse
                    .Builder()
                    .body(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .build()
            }

        eregEksternClient.hentNokkelinfo("test-orgnummer")

        path shouldBe "/v2/organisasjon/test-orgnummer/noekkelinfo"
    }

    @Test
    fun `burde returnere Arbeidsforholdoversikt fra aareg`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .body(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .build()
            }

        eregEksternClient.hentNokkelinfo("_")
    }

    @Test
    fun `burde kaste feil ved error response`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .body(EKSEMPEL_ERROR_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .code(404)
                    .build()
            }

        shouldThrow<RestClientException> {
            eregEksternClient.hentNokkelinfo("_")
        }
    }

    @Test
    fun `burde kaste RuntimeException ved tom respons body`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .addHeader("Content-Type", "application/json")
                    .build()
            }

        shouldThrow<RuntimeException> {
            eregEksternClient.hentNokkelinfo("suksess_uten_body_orgnummer")
        }
    }

    @Test
    fun `hentOrganisasjoner burde kalle med riktig path`() {
        var path: String? = null
        var body: String? = null
        eregMockWebServer.dispatcher =
            simpleDispatcher { request ->
                path = request.url.encodedPath
                body = request.body!!.utf8()
                MockResponse
                    .Builder()
                    .body(EKSEMPEL_HENT_ORGANISASJONER_RESPONSE.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .build()
            }

        eregEksternClient.hentOrganisasjoner(listOf("123456789", "987654321"))

        path shouldBe "/v2/organisasjon/hentOrganisasjoner"
        val requestBody = objectMapper.readValue(body, HentOrganisasjonerRequest::class.java)
        requestBody.organisasjonsnummere shouldBe listOf("123456789", "987654321")
    }

    @Test
    fun `hentOrganisasjoner burde returnere organisasjoner`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .body(EKSEMPEL_HENT_ORGANISASJONER_RESPONSE.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .build()
            }

        val response = eregEksternClient.hentOrganisasjoner(listOf("990983666"))

        response.organisasjoner.size shouldBe 1
        response.organisasjoner["990983666"]!!.navn.sammensattnavn shouldBe "NAV FAMILIE- OG PENSJONSYTELSER OSL"
    }

    @Test
    fun `hentOrganisasjoner burde tillate nullverdier i organisasjonskartet`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .body(
                        """
                        {
                          "organisasjoner": {
                            "990983666": {
                              "navn": {
                                "sammensattnavn": "NAV FAMILIE- OG PENSJONSYTELSER OSL"
                              }
                            },
                            "123456789": null
                          }
                        }
                        """.trimIndent(),
                    ).addHeader("Content-Type", "application/json")
                    .build()
            }

        val response = eregEksternClient.hentOrganisasjoner(listOf("990983666", "123456789"))

        response.organisasjoner.size shouldBe 2
        response.organisasjoner["990983666"]!!.navn.sammensattnavn shouldBe "NAV FAMILIE- OG PENSJONSYTELSER OSL"
        response.organisasjoner.containsKey("123456789") shouldBe true
        response.organisasjoner["123456789"] shouldBe null
    }

    @Test
    fun `hentOrganisasjoner burde avvise null som organisasjonskart`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .body("""{"organisasjoner": null}""")
                    .addHeader("Content-Type", "application/json")
                    .build()
            }

        shouldThrow<RestClientException> {
            eregEksternClient.hentOrganisasjoner(listOf("123456789"))
        }
    }

    @Test
    fun `hentOrganisasjoner burde kaste feil ved error response`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .body(EKSEMPEL_ERROR_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .code(500)
                    .build()
            }

        shouldThrow<RestClientException> {
            eregEksternClient.hentOrganisasjoner(listOf("123"))
        }
    }

    @Test
    fun `hentOrganisasjoner burde kaste RuntimeException ved tom respons body`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .addHeader("Content-Type", "application/json")
                    .build()
            }

        shouldThrow<RuntimeException> {
            eregEksternClient.hentOrganisasjoner(listOf("123"))
        }
    }
}

val EKSEMPEL_ERROR_RESPONSE_FRA_EREG: JsonNode =
    objectMapper.readTree("""{"melding": "Det oppsto en feil!"}""")

val EKSEMPEL_RESPONSE_FRA_EREG: JsonNode =
    objectMapper.readTree(
        """
    {
  "organisasjonsnummer": "990983666",
  "navn": {
    "sammensattnavn": "NAV FAMILIE- OG PENSJONSYTELSER OSL",
    "navnelinje1": "NAV FAMILIE- OG PENSJONSYTELSER",
    "navnelinje2": "OSL",
    "navnelinje3": "string",
    "navnelinje4": "string",
    "navnelinje5": "string",
    "bruksperiode": {
      "fom": "2015-01-06T21:44:04.748",
      "tom": "2015-12-06T19:45:04"
    },
    "gyldighetsperiode": {
      "fom": "2014-07-01",
      "tom": "2015-12-31"
    }
  },
  "enhetstype": "BEDR",
  "adresse": {
    "adresselinje1": "string",
    "adresselinje2": "string",
    "adresselinje3": "string",
    "postnummer": "0557",
    "poststed": "string",
    "landkode": "JPN",
    "kommunenummer": "0301",
    "bruksperiode": {
      "fom": "2015-01-06T21:44:04.748",
      "tom": "2015-12-06T19:45:04"
    },
    "gyldighetsperiode": {
      "fom": "2014-07-01",
      "tom": "2015-12-31"
    },
    "type": "string"
  },
  "opphoersdato": "2016-12-31"
}
""",
    )

val EKSEMPEL_HENT_ORGANISASJONER_RESPONSE: JsonNode =
    objectMapper.readTree(
        """
    {
      "organisasjoner": {
        "990983666": {
          "navn": {
            "sammensattnavn": "NAV FAMILIE- OG PENSJONSYTELSER OSL",
            "navnelinje1": "NAV FAMILIE- OG PENSJONSYTELSER",
            "navnelinje2": "OSL"
          }
        }
      }
    }
    """,
    )
