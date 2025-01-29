package no.nav.helse.flex

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

@TestConfiguration
class MockWebServereConfig {
    companion object {
        val logger = logger()

        init {
            logger.info("[TEST] Starter mock webservere")
        }

        val pdlMockWebServer =
            MockWebServer().apply {
                System.setProperty("PDL_BASE_URL", "http://localhost:$port")
                dispatcher = notFoundDispatcher
            }

        val aaregMockWebServer =
            MockWebServer().apply {
                System.setProperty("AAREG_URL", "http://localhost:$port")
                dispatcher = AaregMockDispatcher
            }

        val eregMockWebServer =
            MockWebServer().apply {
                System.setProperty("EREG_URL", "http://localhost:$port")
                dispatcher = EregMockDispatcher
            }
    }

    @Bean
    fun pdlMockWebServer() = pdlMockWebServer
}
