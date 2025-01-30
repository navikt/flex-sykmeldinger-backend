package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.MockWebServereConfig
import no.nav.helse.flex.arbeidsforhold.innhenting.eregclient.EregClient
import no.nav.helse.flex.defaultEregDispatcher
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.simpleDispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = [EregClient::class, RestTemplate::class, MockWebServereConfig::class])
class EregClientTest {
    @Autowired
    private lateinit var eregMockWebServer: MockWebServer

    @Autowired
    private lateinit var eregClient: EregClient

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

        eregClient.hentNokkelinfo("test-orgnummer")

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

        eregClient.hentNokkelinfo("_")
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
            eregClient.hentNokkelinfo("_")
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
            eregClient.hentNokkelinfo("suksess_uten_body_orgnummer")
        } `should throw` RuntimeException::class
    }
}
