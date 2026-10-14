package no.nav.helse.flex.gateways

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
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
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientException

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
                    MockResponse
                        .Builder()
                        .body(HarSoknadResponse(harSoknad = true).serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                        .build()
                }
            val resultat = sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid", ArbeidssituasjonDTO.NAERINGSDRIVENDE)
            resultat.`should be true`()
        }

        @Test
        fun `burde returnere false når søknad ikke finnes`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse
                        .Builder()
                        .body(HarSoknadResponse(harSoknad = false).serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                        .build()
                }
            val resultat = sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid", ArbeidssituasjonDTO.NAERINGSDRIVENDE)
            resultat.`should be false`()
        }

        @Test
        fun `burde kaste feil ved error response`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse
                        .Builder()
                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .addHeader("Content-Type", "application/json")
                        .build()
                }
            invoking {
                sykepengesoknadBackendEksternClient.harSoknad("sykmelding-uuid", ArbeidssituasjonDTO.NAERINGSDRIVENDE)
            } `should throw` RestClientException::class
        }

        @Test
        fun `burde sende riktig path`() {
            var recordedRequest: RecordedRequest? = null
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    MockResponse
                        .Builder()
                        .body(HarSoknadResponse(harSoknad = true).serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                        .build()
                }

            sykepengesoknadBackendEksternClient.harSoknad("min-sykmelding-id", ArbeidssituasjonDTO.NAERINGSDRIVENDE)

            val request = recordedRequest!!
            request.url.encodedPath `should be equal to` "/api/v2/soknader/sykmelding/min-sykmelding-id/harSoknad/NAERINGSDRIVENDE"
        }

        @Test
        fun `burde sende bearer token i authorization header`() {
            var recordedRequest: RecordedRequest? = null
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    MockResponse
                        .Builder()
                        .body(HarSoknadResponse(harSoknad = true).serialisertTilString())
                        .addHeader("Content-Type", "application/json")
                        .build()
                }

            sykepengesoknadBackendEksternClient.harSoknad("test-id", ArbeidssituasjonDTO.NAERINGSDRIVENDE)

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
                    MockResponse
                        .Builder()
                        .code(HttpStatus.OK.value())
                        .addHeader("Content-Type", "application/json")
                        .build()
                }
            invoking {
                sykepengesoknadBackendEksternClient.opprettOptIn(testMelding)
            } `should not throw` AnyException
        }

        @Test
        fun `burde kaste feil ved error response`() {
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher {
                    MockResponse
                        .Builder()
                        .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .addHeader("Content-Type", "application/json")
                        .build()
                }
            invoking {
                sykepengesoknadBackendEksternClient.opprettOptIn(testMelding)
            } `should throw` RestClientException::class
        }

        @Test
        fun `burde sende til riktig path`() {
            var recordedRequest: RecordedRequest? = null
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    MockResponse
                        .Builder()
                        .code(HttpStatus.OK.value())
                        .addHeader("Content-Type", "application/json")
                        .build()
                }

            sykepengesoknadBackendEksternClient.opprettOptIn(testMelding)

            val request = recordedRequest!!
            request.url.encodedPath `should be equal to` "/api/v2/soknader/opprett-opt-in"
        }

        @Test
        fun `burde sende bearer token i authorization header`() {
            var recordedRequest: RecordedRequest? = null
            sykepengesoknadBackendMockWebServer.dispatcher =
                simpleDispatcher { request ->
                    recordedRequest = request
                    MockResponse
                        .Builder()
                        .code(HttpStatus.OK.value())
                        .addHeader("Content-Type", "application/json")
                        .build()
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
                MockResponse
                    .Builder()
                    .body(HarSoknadResponse(harSoknad = true).serialisertTilString())
                    .addHeader("Content-Type", "application/json")
                    .build()
            }

        sykepengesoknadBackendEksternClient.harSoknad("test-id", ArbeidssituasjonDTO.NAERINGSDRIVENDE)

        val request = recordedRequest!!
        request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
    }
}
