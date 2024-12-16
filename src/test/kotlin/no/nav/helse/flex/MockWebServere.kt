package no.nav.helse.flex

import okhttp3.mockwebserver.MockWebServer
import kotlin.apply

fun startMockWebServere(): MockWebServere {
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

    return MockWebServere(
        eregMockWebServer = eregMockWebServer,
        aaregMockWebServer = aaregMockWebServer,
    )
}

data class MockWebServere(
    val eregMockWebServer: MockWebServer,
    val aaregMockWebServer: MockWebServer,
)
