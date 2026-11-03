package no.nav.helse.flex.gateways.pdl

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultPdlDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.objectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate

@RestClientOppsett
@Import(PdlEksternClient::class)
class PdlClientTest {
    @Autowired
    lateinit var pdlClient: PdlClient

    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

    @AfterEach
    fun cleanUp() {
        pdlMockWebServer.dispatcher = defaultPdlDispatcher
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

            val request = recordedRequest.shouldNotBeNull()
            request.shouldNotBeNull()
            request.headers["Tema"] shouldBe "SYK"
            request.headers["Behandlingsnummer"] shouldBe "B229"
            request.headers["Content-Type"] shouldBe "application/json"
            val parsedBody: GraphQlRequest = objectMapper.readValue(request.body!!.utf8())
            parsedBody.query shouldBeGraphQlQueryEqualTo
                """
                query HentIdenterMedHistorikk(${"$"}ident: ID!) {
                    hentIdenter(ident: ${"$"}ident, historikk: true) {
                        identer {
                            ident,
                            gruppe
                        }
                    }
                }
                """
            parsedBody.variables shouldBe mapOf("ident" to "ident")

            request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
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
            responseData shouldBe listOf(PdlIdent(gruppe = "g1", ident = "i1"), PdlIdent(gruppe = "g2", ident = "i2"))
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
            responseData.size shouldBe 1
            responseData.first().ident shouldBe "ident"
        }
    }

    @Nested
    inner class HentFormattertMetadataNavn {
        @Test
        fun `burde produsere riktig request`() {
            var recordedRequest: RecordedRequest? = null

            pdlMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    lagGraphQlResponse(lagGetPersonResponseData())
                }

            pdlClient.hentFormattertNavn("fnr")

            val request = recordedRequest.shouldNotBeNull()
            request.headers["Behandlingsnummer"] shouldBe "B229"
            request.headers["Tema"] shouldBe "SYK"
            request.headers["Content-Type"] shouldBe "application/json"
            val parsedBody: GraphQlRequest = objectMapper.readValue(request.body!!.utf8())
            parsedBody.query shouldBeGraphQlQueryEqualTo
                """
            query HentPersonNavn(${'$'}ident: ID!) {
                hentPerson(ident: ${'$'}ident) {
                    navn(historikk: false) {
                        fornavn
                        mellomnavn
                        etternavn
                    }
                }
            }
            """
            parsedBody.variables shouldBe mapOf("ident" to "fnr")

            request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
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
            responseData shouldBe "Navn Mellom Navnesen"
        }

        @Test
        fun `burde handtere kapitalisering riktig`() {
            pdlMockWebServer.dispatcher =
                simpleDispatcher {
                    lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Tor-Henry", etternavn = "Roarsen"))
                }

            val responseData = pdlClient.hentFormattertNavn("00888888821")
            responseData shouldBe "Tor-Henry Roarsen"
        }

        @Test
        fun `burde handtere æøå riktig`() {
            pdlMockWebServer.dispatcher =
                simpleDispatcher {
                    lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Åge Roger", etternavn = "Åæøå"))
                }

            val responseData = pdlClient.hentFormattertNavn("00333888821")
            responseData shouldBe "Åge Roger Åæøå"
        }
    }

    @Nested
    inner class HentFoedselsdato {
        @Test
        fun `burde produsere riktig request`() {
            var recordedRequest: RecordedRequest? = null

            pdlMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    lagGraphQlResponse(lagGetPersonResponseData())
                }

            pdlClient.hentFoedselsdato("fnr")

            val request = recordedRequest.shouldNotBeNull()
            request.headers["Behandlingsnummer"] shouldBe "B229"
            request.headers["Tema"] shouldBe "SYK"
            request.headers["Content-Type"] shouldBe "application/json"
            val parsedBody: GraphQlRequest = objectMapper.readValue(request.body!!.utf8())
            parsedBody.query shouldBeGraphQlQueryEqualTo
                """
                query HentPersonFoedselsdato(${"$"}ident: ID!) {
                  hentPerson(ident: ${"$"}ident) {
                    foedselsdato {
                      foedselsdato
                    }
                  }
                }
            """
            parsedBody.variables shouldBe mapOf("ident" to "fnr")

            request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
        }

        @Test
        fun `burde svare med riktig fødselsdato`() {
            pdlMockWebServer.dispatcher =
                simpleDispatcher {
                    lagGraphQlResponse(
                        lagGetPersonResponseData(
                            foedselsdato = "2000-01-01",
                        ),
                    )
                }

            val responseData = pdlClient.hentFoedselsdato("fnr")
            responseData shouldBe LocalDate.parse("2000-01-01")
        }
    }
}

private infix fun String.shouldBeGraphQlQueryEqualTo(expected: String) {
    this.standariserGraphQlQuery() shouldBe expected.standariserGraphQlQuery()
}

private fun String.standariserGraphQlQuery() =
    this
        .split("\n")
        .filter { it.isNotBlank() }
        .joinToString("\n") { it.trim() }
