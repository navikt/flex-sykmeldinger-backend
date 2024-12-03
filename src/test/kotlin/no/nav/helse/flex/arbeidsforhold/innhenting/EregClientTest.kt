package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.innhenting.eregclient.EregClient
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.QueueDispatcher
import okhttp3.mockwebserver.RecordedRequest
import org.amshove.kluent.invoking
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [EregClient::class, RestTemplate::class])
class EregClientTest {
    @Autowired
    private lateinit var eregClient: EregClient

    init {
        MockWebServer().apply {
            System.setProperty("EREG_URL", "http://localhost:$port")
            dispatcher = EregMockDispatcher
        }
    }

    @Test
    fun `burde returnere Arbeidsforholdoversikt fra aareg`() {
        eregClient.hentNokkelinfo("suksess_orgnummer")
    }

    @Test
    fun `burde kaste feil ved error response`() {
        invoking {
            eregClient.hentNokkelinfo("feilmeldig_orgnummer")
        } `should throw` RestClientException::class
    }

    @Test
    fun `burde kaste RuntimeException ved tom respons body`() {
        invoking {
            eregClient.hentNokkelinfo("suksess_uten_body_orgnummer")
        } `should throw` RuntimeException::class
    }
}

private object EregMockDispatcher : QueueDispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return when (request.path) {
            "/v2/organisasjon/feilmeldig_orgnummer/noekkelinfo" -> {
                MockResponse()
                    .setBody(EKSEMPEL_ERROR_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(404)
            }

            "/v2/organisasjon/suksess_uten_body_orgnummer/noekkelinfo" -> {
                MockResponse()
                    .addHeader("Content-Type", "application/json")
            }

            "/v2/organisasjon/suksess_orgnummer/noekkelinfo" -> {
                MockResponse().setBody(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }

            else -> {
                error("Uhåndtert ${request.path}")
            }
        }
    }
}

private val EKSEMPEL_ERROR_RESPONSE_FRA_EREG =
    objectMapper.readTree("""{"melding": "Det oppsto en feil!"}""")

private val EKSEMPEL_RESPONSE_FRA_EREG =
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
