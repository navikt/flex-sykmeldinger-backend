package no.nav.helse.flex

import no.nav.helse.flex.mockdispatcher.PdlMockDispatcher
import no.nav.helse.flex.pdl.*
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PdlClientTest : FellesTestOppsett() {
    @Autowired
    private lateinit var pdlClient: PdlClient

    final val fnr = "12345678910"

    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

    private lateinit var dispatcher: PdlMockDispatcher

    @BeforeAll
    fun setUp() {
        dispatcher = PdlMockDispatcher
        pdlMockWebServer.dispatcher = dispatcher
    }

    @Test
    fun `Vi tester happycase`() {
        val responseData = pdlClient.hentFormattertNavn(fnr)

        responseData `should be equal to` "Ole Gunnar"

        val request = dispatcher.requester.last()
        request.headers["Behandlingsnummer"] `should be equal to` "B128"
        request.headers["Tema"] `should be equal to` "SYK"
        val parsedBody = GraphQlRequest.fraJson(request.body)
        parsedBody.query shouldBeGraphQlQueryEqualTo
            """
            query(${'$'}ident: ID!) {
                hentPerson(ident: ${'$'}ident) {
                    navn(historikk: false) {
                        fornavn
                        mellomnavn
                        etternavn
                    }
                }
            }
            """
        parsedBody.variables `should be equal to` mapOf("ident" to "12345678910")

        request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
    }

    @Test
    fun `Tor-Henry blir riktig kapitalisert`() {
        val responseData = pdlClient.hentFormattertNavn("00888888821")
        responseData `should be equal to` "Tor-Henry Roarsen"
    }

    @Test
    fun `æøå blir riktig`() {
        val responseData = pdlClient.hentFormattertNavn("00333888821")
        responseData `should be equal to` "Åge Roger Åæøå"
    }
}

private infix fun String.shouldBeGraphQlQueryEqualTo(expected: String) {
    this.standariserGraphQlQuery() `should be equal to` expected.standariserGraphQlQuery()
}

private fun String.standariserGraphQlQuery() =
    this
        .split("\n")
        .filter { it.isNotBlank() }
        .map { it.trim() }
        .joinToString("\n")
