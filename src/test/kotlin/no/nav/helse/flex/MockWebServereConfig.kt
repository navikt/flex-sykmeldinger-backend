package no.nav.helse.flex

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import kotlin.apply

val noopDispatcher =
    object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = MockResponse().setResponseCode(404)
    }

@TestConfiguration
class MockWebServereConfig {
    companion object {
        init {
            println("Starting mock webservers")
        }

        val pdlMockWebServer =
            MockWebServer().apply {
                System.setProperty("PDL_BASE_URL", "http://localhost:$port")
                dispatcher = noopDispatcher
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
