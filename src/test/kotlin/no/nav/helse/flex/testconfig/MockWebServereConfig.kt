package no.nav.helse.flex.testconfig

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.EKSEMPEL_RESPONSE_FRA_EREG
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import kotlin.apply

fun simpleDispatcher(dispatcherFunc: (RecordedRequest) -> MockResponse): Dispatcher =
    object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = dispatcherFunc(request)
    }

val notFoundDispatcher = simpleDispatcher { MockResponse().setResponseCode(404) }

val defaultAaregDispatcher =
    simpleDispatcher {
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(lagArbeidsforholdOversiktResponse(arbeidsforholdoversikter = emptyList()).serialisertTilString())
    }

val defaultEregDispatcher =
    simpleDispatcher {
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString().serialisertTilString())
    }

val defaultPdlDispatcher = notFoundDispatcher

@TestConfiguration
class MockWebServereConfig {
    @Bean
    fun pdlMockWebServer() = pdlMockWebServer

    @Bean
    fun aaregMockWebServer() = aaregMockWebServer

    @Bean
    fun eregMockWebServer() = eregMockWebServer

    companion object {
        val logger = logger()

        init {
            logger.info("[TEST] Starter mock webservere")
        }

        val pdlMockWebServer =
            MockWebServer().apply {
                System.setProperty("PDL_BASE_URL", "http://localhost:$port")
                dispatcher = defaultPdlDispatcher
            }

        val aaregMockWebServer =
            MockWebServer().apply {
                System.setProperty("AAREG_URL", "http://localhost:$port")
                dispatcher = defaultAaregDispatcher
            }

        val eregMockWebServer =
            MockWebServer().apply {
                System.setProperty("EREG_URL", "http://localhost:$port")
                dispatcher = defaultEregDispatcher
            }
    }
}
