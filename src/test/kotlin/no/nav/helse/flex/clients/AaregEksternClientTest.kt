package no.nav.helse.flex.clients

import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.clients.aareg.AaregEksternClient
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultAaregDispatcher
import no.nav.helse.flex.testconfig.singlePathDispatcher
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientException

@RestClientOppsett
@Import(AaregEksternClient::class)
class AaregEksternClientTest {
    @Autowired
    private lateinit var aaregMockWebServer: MockWebServer

    @Autowired
    private lateinit var aaregEksternClient: AaregClient

    @AfterEach
    fun afterEach() {
        aaregMockWebServer.dispatcher = defaultAaregDispatcher
    }

    @Nested
    inner class GetArbeidstakerArbeidsforholdoversikt {
        @Test
        fun `burde returnere Arbeidsforholdoversikt fra aareg`() {
            aaregMockWebServer.dispatcher =
                singlePathDispatcher("/api/v2/arbeidstaker/arbeidsforholdoversikt") {
                    MockResponse()
                        .setBody(EKSEMPEL_RESPONSE_FRA_AAREG.serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                }
            aaregEksternClient.getArbeidstakerArbeidsforholdoversikt("_") `should not be` null
        }

        @Test
        fun `burde kaste feil ved error response`() {
            aaregMockWebServer.dispatcher =
                singlePathDispatcher("/api/v2/arbeidstaker/arbeidsforholdoversikt") {
                    MockResponse()
                        .setBody(EKSEMPEL_ERROR_RESPONSE_FRA_AAREG.serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(HttpStatus.NOT_FOUND.value())
                }

            invoking {
                aaregEksternClient.getArbeidstakerArbeidsforholdoversikt("_")
            } `should throw` RestClientException::class
        }

        @Test
        fun `burde kaste RuntimeException ved tom respons body`() {
            aaregMockWebServer.dispatcher =
                singlePathDispatcher("/api/v2/arbeidstaker/arbeidsforholdoversikt") {
                    MockResponse()
                        .addHeader("Content-Type", "application/json")
                }
            invoking {
                aaregEksternClient.getArbeidstakerArbeidsforholdoversikt("suksess_uten_body_fnr")
            } `should throw` RuntimeException::class
        }

        @Test
        fun `burde ha riktig headers`() {
            var recordedReq: RecordedRequest? = null
            aaregMockWebServer.dispatcher =
                singlePathDispatcher("/api/v2/arbeidstaker/arbeidsforholdoversikt") { req ->
                    recordedReq = req
                    MockResponse()
                        .setBody(EKSEMPEL_RESPONSE_FRA_AAREG.serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                }

            aaregEksternClient.getArbeidstakerArbeidsforholdoversikt("_")
            recordedReq.shouldNotBeNull().run {
                headers["Content-Type"] `should be equal to` "application/json"
            }
        }
    }

    @Nested
    inner class GetArbeidsstedArbeidsforholdoversikt {
        @Test
        fun `burde returnere Arbeidsforholdoversikt fra aareg`() {
            aaregMockWebServer.dispatcher =
                singlePathDispatcher("/api/v2/arbeidssted/arbeidsforholdoversikt") {
                    MockResponse()
                        .setBody(EKSEMPEL_RESPONSE_FRA_AAREG.serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                }
            aaregEksternClient.getArbeidsstedArbeidsforholdoversikt("_") `should not be` null
        }

        @Test
        fun `burde kaste feil ved error response`() {
            aaregMockWebServer.dispatcher =
                singlePathDispatcher("/api/v2/arbeidssted/arbeidsforholdoversikt") {
                    MockResponse()
                        .setBody(EKSEMPEL_ERROR_RESPONSE_FRA_AAREG.serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                        .setResponseCode(HttpStatus.NOT_FOUND.value())
                }

            invoking {
                aaregEksternClient.getArbeidsstedArbeidsforholdoversikt("_")
            } `should throw` RestClientException::class
        }

        @Test
        fun `burde kaste RuntimeException ved tom respons body`() {
            aaregMockWebServer.dispatcher =
                singlePathDispatcher("/api/v2/arbeidssted/arbeidsforholdoversikt") {
                    MockResponse()
                        .addHeader("Content-Type", "application/json")
                }
            invoking {
                aaregEksternClient.getArbeidsstedArbeidsforholdoversikt("_")
            } `should throw` RuntimeException::class
        }

        @Test
        fun `burde ha riktig headers`() {
            var recordedReq: RecordedRequest? = null
            aaregMockWebServer.dispatcher =
                singlePathDispatcher("/api/v2/arbeidssted/arbeidsforholdoversikt") { req ->
                    recordedReq = req
                    MockResponse()
                        .setBody(EKSEMPEL_RESPONSE_FRA_AAREG.serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                }

            aaregEksternClient.getArbeidsstedArbeidsforholdoversikt("_")
            recordedReq.shouldNotBeNull().run {
                headers["Content-Type"] `should be equal to` "application/json"
            }
        }
    }
}

private val EKSEMPEL_ERROR_RESPONSE_FRA_AAREG =
    objectMapper.readTree(
        """
    {
  "meldinger": [
    "Det oppsto en feil!"
  ]
}
""",
    )

private val EKSEMPEL_RESPONSE_FRA_AAREG =
    objectMapper.readTree(
        """
    {
  "arbeidsforholdoversikter": [
    {
      "type": {
        "kode": "ordinaertArbeidsforhold",
        "beskrivelse": "Ordinært arbeidsforhold"
      },
      "arbeidstaker": {
        "identer": [
          {
            "type": "AKTORID",
            "ident": "2175141353812",
            "gjeldende": true
          },
          {
            "type": "FOLKEREGISTERIDENT",
            "ident": "30063000562",
            "gjeldende": true
          }
        ]
      },
      "arbeidssted": {
        "type": "Underenhet",
        "identer": [
          {
            "type": "ORGANISASJONSNUMMER",
            "ident": "910825518"
          }
        ]
      },
      "opplysningspliktig": {
        "type": "Hovedenhet",
        "identer": [
          {
            "type": "ORGANISASJONSNUMMER",
            "ident": "810825472"
          }
        ]
      },
      "startdato": "2014-01-01",
      "yrke": {
        "kode": "1231119",
        "beskrivelse": "KONTORLEDER"
      },
      "avtaltStillingsprosent": 100,
      "permisjonsprosent": 50,
      "permitteringsprosent": 50,
      "rapporteringsordning": {
        "kode": "a-ordningen",
        "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
      },
      "navArbeidsforholdId": 12345,
      "sistBekreftet": "2020-09-15T08:19:53"
    },
    {
      "type": {
        "kode": "ordinaertArbeidsforhold",
        "beskrivelse": "Ordinært arbeidsforhold"
      },
      "arbeidstaker": {
        "identer": [
          {
            "type": "AKTORID",
            "ident": "2175141353812",
            "gjeldende": true
          },
          {
            "type": "FOLKEREGISTERIDENT",
            "ident": "30063000562",
            "gjeldende": true
          }
        ]
      },
      "arbeidssted": {
        "type": "Underenhet",
        "identer": [
          {
            "type": "ORGANISASJONSNUMMER",
            "ident": "972674818"
          }
        ]
      },
      "opplysningspliktig": {
        "type": "Hovedenhet",
        "identer": [
          {
            "type": "ORGANISASJONSNUMMER",
            "ident": "928497704"
          }
        ]
      },
      "startdato": "2020-01-01",
      "yrke": {
        "kode": "1233101",
        "beskrivelse": "ABONNEMENTSJEF"
      },
      "avtaltStillingsprosent": 100,
      "varsler": [
        {
          "entitet": "Arbeidsforhold",
          "varslingskode": {
            "kode": "AFIDHI",
            "beskrivelse": "Arbeidsforholdet har id-historikk"
          }
        }
      ],
      "rapporteringsordning": {
        "kode": "a-ordningen",
        "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
      },
      "navArbeidsforholdId": 45678,
      "sistBekreftet": "2020-07-28T09:10:19"
    },
    {
      "type": {
        "kode": "maritimtArbeidsforhold",
        "beskrivelse": "Maritimt arbeidsforhold"
      },
      "arbeidstaker": {
        "identer": [
          {
            "type": "AKTORID",
            "ident": "2175141353812",
            "gjeldende": true
          },
          {
            "type": "FOLKEREGISTERIDENT",
            "ident": "30063000562",
            "gjeldende": true
          }
        ]
      },
      "arbeidssted": {
        "type": "Underenhet",
        "identer": [
          {
            "type": "ORGANISASJONSNUMMER",
            "ident": "896929119"
          }
        ]
      },
      "opplysningspliktig": {
        "type": "Hovedenhet",
        "identer": [
          {
            "type": "ORGANISASJONSNUMMER",
            "ident": "928497704"
          }
        ]
      },
      "startdato": "2012-03-15",
      "yrke": {
        "kode": "6411104",
        "beskrivelse": "FISKER"
      },
      "avtaltStillingsprosent": 100,
      "rapporteringsordning": {
        "kode": "a-ordningen",
        "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
      },
      "navArbeidsforholdId": 23456,
      "sistBekreftet": "2021-01-01T19:25:17"
    },
    {
      "type": {
        "kode": "forenkletOppgjoersordning",
        "beskrivelse": "Forenklet oppgjørsordning"
      },
      "arbeidstaker": {
        "identer": [
          {
            "type": "AKTORID",
            "ident": "2175141353812",
            "gjeldende": true
          },
          {
            "type": "FOLKEREGISTERIDENT",
            "ident": "30063000562",
            "gjeldende": true
          }
        ]
      },
      "arbeidssted": {
        "type": "Person",
        "identer": [
          {
            "type": "AKTORID",
            "ident": "2005579084646",
            "gjeldende": true
          },
          {
            "type": "FOLKEREGISTERIDENT",
            "ident": "19095901754",
            "gjeldende": true
          }
        ]
      },
      "opplysningspliktig": {
        "type": "Person",
        "identer": [
          {
            "type": "AKTORID",
            "ident": "2005579084646",
            "gjeldende": true
          },
          {
            "type": "FOLKEREGISTERIDENT",
            "ident": "19095901754",
            "gjeldende": true
          }
        ]
      },
      "startdato": "2020-01-01",
      "sluttdato": "2020-01-03",
      "yrke": {
        "kode": "9120105",
        "beskrivelse": "ALTMULIGMANN (PRIVATHJEM)"
      },
      "rapporteringsordning": {
        "kode": "a-ordningen",
        "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
      },
      "navArbeidsforholdId": 34567,
      "sistBekreftet": "2020-06-10T11:03:47"
    },
    {
      "type": {
        "kode": "frilanserOppdragstakerHonorarPersonerMm",
        "beskrivelse": "Frilansere/oppdragstakere, styremedlemmer, folkevalgte, personer som innehar tillitsverv, fosterforelder, støttekontakter, avlastere og personer med omsorgslønn"
      },
      "arbeidstaker": {
        "identer": [
          {
            "type": "AKTORID",
            "ident": "2175141353812",
            "gjeldende": true
          },
          {
            "type": "FOLKEREGISTERIDENT",
            "ident": "30063000562",
            "gjeldende": true
          }
        ]
      },
      "arbeidssted": {
        "type": "Underenhet",
        "identer": [
          {
            "type": "ORGANISASJONSNUMMER",
            "ident": "824771332"
          }
        ]
      },
      "opplysningspliktig": {
        "type": "Hovedenhet",
        "identer": [
          {
            "type": "ORGANISASJONSNUMMER",
            "ident": "928497704"
          }
        ]
      },
      "startdato": "2020-01-01",
      "sluttdato": "2021-04-30",
      "yrke": {
        "kode": "1210160",
        "beskrivelse": "STYREMEDLEM"
      },
      "avtaltStillingsprosent": 0,
      "rapporteringsordning": {
        "kode": "a-ordningen",
        "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
      },
      "navArbeidsforholdId": 56789,
      "sistBekreftet": "2020-06-10T11:03:47"
    }
  ],
  "totalAntall": 5
}
""",
    )
