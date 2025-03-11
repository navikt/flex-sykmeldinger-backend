package no.nav.helse.flex.clients

import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.clients.aareg.AaregEksternClient
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultAaregDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should not be`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource

@RestClientOppsett
@Import(AaregEksternClient::class, EnvironmentToggles::class)
@TestPropertySource(properties = ["NAIS_CLUSTER_NAME= dev-gcp"])
class AaregClientTestDev {
    @Autowired
    private lateinit var aaregMockWebServer: MockWebServer

    @Autowired
    private lateinit var aaregEksternClient: AaregClient

    @AfterEach
    fun afterEach() {
        aaregMockWebServer.dispatcher = defaultAaregDispatcher
    }

    @Test
    fun `burde ignorere at aareg er nede i dev`() {
        aaregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .addHeader("Content-Type", "application/json")
                    .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
            }
        aaregEksternClient.getArbeidsforholdoversikt("_") `should not be` null
    }
}
