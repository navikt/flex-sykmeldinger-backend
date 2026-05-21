package no.nav.helse.flex.gateways

import no.nav.helse.flex.gateways.sykepengesoknadbackend.HarSoknadResponse
import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient
import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendEksternClient
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultSykepengesoknadBackendDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import java.util.concurrent.TimeUnit

@RestClientOppsett
@Import(SykepengesoknadBackendEksternClient::class)
class SykepengesoknadBackendClientTest {
    @Autowired
    lateinit var sykepengesoknadBackendMockWebServer: MockWebServer

    @Autowired
    lateinit var sykepengesoknadBackendEksternClient: SykepengesoknadBackendClient

    @AfterEach
    fun afterEach() {
        sykepengesoknadBackendMockWebServer.dispatcher = defaultSykepengesoknadBackendDispatcher
    }

    @Test
    fun `burde returnere true når søknad finnes`() {
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(HarSoknadResponse(harSoknad = true).serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }
        val resultat = sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
        resultat.`should be true`()
    }

    @Test
    fun `burde returnere false når søknad ikke finnes`() {
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(HarSoknadResponse(harSoknad = false).serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }
        val resultat = sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
        resultat.`should be false`()
    }

    @Test
    fun `burde returnere false ved 401`() {
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                    .addHeader("Content-Type", "application/json")
            }
        val resultat = sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
        resultat.`should be false`()
    }

    @Test
    fun `burde returnere false ved 403`() {
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setResponseCode(HttpStatus.FORBIDDEN.value())
                    .addHeader("Content-Type", "application/json")
            }
        val resultat = sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
        resultat.`should be false`()
    }

    @Test
    fun `burde sende riktig path`() {
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(HarSoknadResponse(harSoknad = true).serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }

        // Tøm eventuelle tidligere forespørsler som ligger i køen, slik at
        // takeRequest() returnerer requesten fra dette testkallet.
        while (sykepengesoknadBackendMockWebServer.takeRequest(10, TimeUnit.MILLISECONDS) != null) {
            // discard
        }

        sykepengesoknadBackendEksternClient.harSoknad("min-sykmelding-id")

        val request = sykepengesoknadBackendMockWebServer.takeRequest()
        request.path `should be equal to` "/api/v2/soknader/sykmelding/min-sykmelding-id/harSoknad"
    }
}
