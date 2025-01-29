package no.nav.helse.flex.pdl

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.notFoundDispatcher
import no.nav.helse.flex.simpleDispatcher
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PdlClientTest : FellesTestOppsett() {
    @Autowired
    lateinit var pdlClient: PdlClient

    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

    @AfterEach
    fun cleanUp() {
        pdlMockWebServer.dispatcher = notFoundDispatcher
    }

    @Nested
    inner class HentIdenterMedHistorikk {
        @Test
        fun `burde produsere riktig request`() {
            var recordedRequest: RecordedRequest? = null

            pdlMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    lagGraphQlResponse(lagHentIdenterResponseData())
                }

            pdlClient.hentIdenterMedHistorikk(ident = "ident")

            recordedRequest.shouldNotBeNull()
            recordedRequest.headers["Tema"] `should be equal to` "SYK"
            recordedRequest.headers["Behandlingsnummer"] `should be equal to` "B229"
            val parsedBody = GraphQlRequest.fraJson(recordedRequest.body.readUtf8())
            parsedBody.query shouldBeGraphQlQueryEqualTo
                """
                query(${"$"}ident: ID!) {
                    hentIdenter(ident: ${"$"}ident, historikk: true) {
                        identer {
                            ident,
                            gruppe
                        }
                    }
                }
                """
            parsedBody.variables `should be equal to` mapOf("ident" to "ident")

            recordedRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")
        }

        @Test
        fun `burde svare med riktig identer`() {
            pdlMockWebServer.dispatcher =
                simpleDispatcher {
                    lagGraphQlResponse(
                        lagHentIdenterResponseData(
                            identer =
                                listOf(
                                    PdlIdent(gruppe = "g1", ident = "i1"),
                                    PdlIdent(gruppe = "g2", ident = "i2"),
                                ),
                        ),
                    )
                }

            val responseData = pdlClient.hentIdenterMedHistorikk("ident")
            responseData `should be equal to` listOf(PdlIdent(gruppe = "g1", ident = "i1"), PdlIdent(gruppe = "g2", ident = "i2"))
        }

        @Test
        fun `burde inkludere ident som blir spurt paa, dersom den er registrert`() {
            pdlMockWebServer.dispatcher =
                simpleDispatcher {
                    lagGraphQlResponse(
                        lagHentIdenterResponseData(
                            identer = listOf(PdlIdent(gruppe = "g", ident = "ident")),
                        ),
                    )
                }

            val responseData = pdlClient.hentIdenterMedHistorikk("ident")
            responseData.size `should be equal to` 1
            responseData.first().ident `should be equal to` "ident"
        }
    }

    @Nested
    inner class HentFormattertNavn {
        @Test
        fun `burde produsere riktig request`() {
            var recordedRequest: RecordedRequest? = null

            pdlMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    lagGraphQlResponse(lagGetPersonResponseData())
                }

            pdlClient.hentFormattertNavn("fnr")

            recordedRequest.shouldNotBeNull()
            recordedRequest.headers["Behandlingsnummer"] `should be equal to` "B229"
            recordedRequest.headers["Tema"] `should be equal to` "SYK"
            val parsedBody = GraphQlRequest.fraJson(recordedRequest.body.readUtf8())
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
            parsedBody.variables `should be equal to` mapOf("ident" to "fnr")

            recordedRequest.headers["Authorization"]!!.shouldStartWith("Bearer ey")
        }

        @Test
        fun `burde svare med riktig navn`() {
            pdlMockWebServer.dispatcher =
                simpleDispatcher {
                    lagGraphQlResponse(
                        lagGetPersonResponseData(
                            fornavn = "Navn",
                            mellomnavn = "Mellom",
                            etternavn = "Navnesen",
                        ),
                    )
                }

            val responseData = pdlClient.hentFormattertNavn("fnr")
            responseData `should be equal to` "Navn Mellom Navnesen"
        }

        @Test
        fun `burde handtere kapitalisering riktig`() {
            pdlMockWebServer.dispatcher =
                simpleDispatcher {
                    lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Tor-Henry", etternavn = "Roarsen"))
                }

            val responseData = pdlClient.hentFormattertNavn("00888888821")
            responseData `should be equal to` "Tor-Henry Roarsen"
        }

        @Test
        fun `burde handtere æøå riktig`() {
            pdlMockWebServer.dispatcher =
                simpleDispatcher {
                    lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Åge Roger", etternavn = "Åæøå"))
                }

            val responseData = pdlClient.hentFormattertNavn("00333888821")
            responseData `should be equal to` "Åge Roger Åæøå"
        }
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
