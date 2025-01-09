package no.nav.helse.flex

import no.nav.helse.flex.mockdispatcher.PdlMockDispatcher
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

    val pdlMockWebServer =
        MockWebServer().apply {
            System.setProperty("PDL_BASE_URL", "http://localhost:$port")
            dispatcher = PdlMockDispatcher
        }

    return MockWebServere(
        eregMockWebServer = eregMockWebServer,
        aaregMockWebServer = aaregMockWebServer,
        pdlMockWebServer = pdlMockWebServer,
    )
}

data class MockWebServere(
    val eregMockWebServer: MockWebServer,
    val aaregMockWebServer: MockWebServer,
    val pdlMockWebServer: MockWebServer,
)
