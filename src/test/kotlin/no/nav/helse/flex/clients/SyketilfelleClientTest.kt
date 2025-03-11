package no.nav.helse.flex.clients

import no.nav.helse.flex.clients.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.clients.syketilfelle.SyketilfelleEksternClient
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultSyketilfelleDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be true`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientException

@RestClientOppsett
@Import(SyketilfelleEksternClient::class)
class SyketilfelleClientTest {
    @Autowired
    lateinit var syketilfelleMockWebServer: MockWebServer

    @Autowired
    lateinit var syketilfelleEksternClient: SyketilfelleClient

    @AfterEach
    fun afterEach() {
        syketilfelleMockWebServer.dispatcher = defaultSyketilfelleDispatcher
    }

    @Test
    fun `burde returnere svar på om sykmelding er utenfor ventetid`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(true.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }
        syketilfelleEksternClient.getErUtenforVentetid(PersonIdenter("fnr"), "sykmeldingId").`should be true`()
    }

    @Test
    fun `burde kaste feil ved error i flex-syketilfelle`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(false.serialisertTilString())
                    .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .addHeader("Content-Type", "application/json")
            }
        invoking {
            syketilfelleEksternClient.getErUtenforVentetid(PersonIdenter("fnr"), "sykmeldingId")
        } `should throw` RestClientException::class
    }

    @Test
    fun `burde kaste feil ved tom body`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .addHeader("Content-Type", "application/json")
            }
        invoking {
            syketilfelleEksternClient.getErUtenforVentetid(PersonIdenter("fnr"), "sykmeldingId")
        } `should throw` RuntimeException::class
    }
}
