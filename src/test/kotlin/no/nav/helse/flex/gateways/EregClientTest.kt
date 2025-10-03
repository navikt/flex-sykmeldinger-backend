package no.nav.helse.flex.gateways

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.flex.gateways.ereg.EregClient
import no.nav.helse.flex.gateways.ereg.EregEksternClient
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultEregDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestClientException

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
                path = request.path
                MockResponse()
                    .setBody(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }

        eregEksternClient.hentNokkelinfo("test-orgnummer")

        path `should be equal to` "/v2/organisasjon/test-orgnummer/noekkelinfo"
    }

    @Test
    fun `burde returnere Arbeidsforholdoversikt fra aareg`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }

        eregEksternClient.hentNokkelinfo("_")
    }

    @Test
    fun `burde kaste feil ved error response`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(EKSEMPEL_ERROR_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(404)
            }

        invoking {
            eregEksternClient.hentNokkelinfo("_")
        } `should throw` RestClientException::class
    }

    @Test
    fun `burde kaste RuntimeException ved tom respons body`() {
        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .addHeader("Content-Type", "application/json")
            }

        invoking {
            eregEksternClient.hentNokkelinfo("suksess_uten_body_orgnummer")
        } `should throw` RuntimeException::class
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
