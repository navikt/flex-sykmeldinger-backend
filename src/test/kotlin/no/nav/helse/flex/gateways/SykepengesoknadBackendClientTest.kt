package no.nav.helse.flex.gateways

import no.nav.helse.flex.gateways.sykepengesoknadbackend.HarSoknadResponse
import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient
import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendEksternClient
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultSykepengesoknadBackendDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.testdata.lagKafkaMetadataDTO
import no.nav.helse.flex.testdata.lagSykmeldingDto
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaDTO
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus

@RestClientOppsett
@Import(SykepengesoknadBackendEksternClient::class)
class SykepengesoknadBackendClientTest {
    @Autowired
    lateinit var sykepengesoknadBackendMockWebServer: MockWebServer

    @Autowired
    lateinit var sykepengesoknadBackendEksternClient: SykepengesoknadBackendClient

    @AfterEach
    fun afterEach() {
        sykepengesoknadBackendMockWebServer.dispatcher = defaultSykepengesoknadBackendDispatcher
    }

    @Nested
    inner class HarSoknad {
        @Test
        fun `burde returnere true når søknad finnes`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse()
                        .setBody(HarSoknadResponse(harSoknad = true).serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                }
            val resultat = sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
            resultat.`should be true`()
        }

        @Test
        fun `burde returnere false når søknad ikke finnes`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse()
                        .setBody(HarSoknadResponse(harSoknad = false).serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                }
            val resultat = sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
            resultat.`should be false`()
        }

        @Test
        fun `burde kaste feil ved 401`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse()
                        .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                        .addHeader("Content-Type", "application/json")
                }
            invoking {
                sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
            } `should throw` RuntimeException::class
        }

        @Test
        fun `burde kaste feil ved 403`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse()
                        .setResponseCode(HttpStatus.FORBIDDEN.value())
                        .addHeader("Content-Type", "application/json")
                }
            invoking {
                sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
            } `should throw` RuntimeException::class
        }

        @Test
        fun `burde kaste feil ved tom body`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse()
                        .addHeader("Content-Type", "application/json")
                }
            invoking {
                sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid")
            } `should throw` RuntimeException::class
        }

        @Test
        fun `burde sende riktig path`() {
            var recordedRequest: RecordedRequest? = null
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    MockResponse()
                        .setBody(HarSoknadResponse(harSoknad = true).serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                }

            sykepengesoknadBackendEksternClient.harSoknad("min-sykmelding-id")

            val request = recordedRequest!!
            request.path `should be equal to` "/api/v2/soknader/sykmelding/min-sykmelding-id/harSoknad"
        }

        @Test
        fun `burde sende bearer token i authorization header`() {
            var recordedRequest: RecordedRequest? = null
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    MockResponse()
                        .setBody(HarSoknadResponse(harSoknad = true).serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                }

            sykepengesoknadBackendEksternClient.harSoknad("test-id")

            val request = recordedRequest!!
            request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
        }
    }

    @Nested
    inner class OpprettOptIn {
        private val testMelding =
            SykmeldingKafkaMessage(
                sykmelding = lagSykmeldingDto(id = "opt-in-sykmelding-id"),
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "opt-in-sykmelding-id"),
                event = lagSykmeldingStatusKafkaDTO(sykmeldingId = "opt-in-sykmelding-id"),
            )

        @Test
        fun `burde fullføre uten feil ved 200`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse()
                        .setResponseCode(HttpStatus.OK.value())
                        .addHeader("Content-Type", "application/json")
                }
            invoking {
                sykepengesoknadBackendEksternClient.opprettOptIn(testMelding)
            } `should not throw` AnyException
        }

        @Test
        fun `burde kaste feil ved 401`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse()
                        .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                        .addHeader("Content-Type", "application/json")
                }
            invoking {
                sykepengesoknadBackendEksternClient.opprettOptIn(testMelding)
            } `should throw` RuntimeException::class
        }

        @Test
        fun `burde kaste feil ved 403`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse()
                        .setResponseCode(HttpStatus.FORBIDDEN.value())
                        .addHeader("Content-Type", "application/json")
                }
            invoking {
                sykepengesoknadBackendEksternClient.opprettOptIn(testMelding)
            } `should throw` RuntimeException::class
        }

        @Test
        fun `burde sende til riktig path`() {
            var recordedRequest: RecordedRequest? = null
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    MockResponse()
                        .setResponseCode(HttpStatus.OK.value())
                        .addHeader("Content-Type", "application/json")
                }

            sykepengesoknadBackendEksternClient.opprettOptIn(testMelding)

            val request = recordedRequest!!
            request.path `should be equal to` "/api/v2/soknader/opprett-opt-in"
        }

        @Test
        fun `burde sende bearer token i authorization header`() {
            var recordedRequest: RecordedRequest? = null
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    MockResponse()
                        .setResponseCode(HttpStatus.OK.value())
                        .addHeader("Content-Type", "application/json")
                }

            sykepengesoknadBackendEksternClient.opprettOptIn(testMelding)

            val request = recordedRequest!!
            request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
        }
    }

    @Test
    fun `burde sende bearer token i authorization header`() {
        var recordedRequest: RecordedRequest? = null
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher { request ->
                recordedRequest = request
                MockResponse()
                    .setBody(HarSoknadResponse(harSoknad = true).serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }

        sykepengesoknadBackendEksternClient.harSoknad("test-id")

        val request = recordedRequest!!
        request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
    }
}
