package no.nav.helse.flex.gateways

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import no.nav.helse.flex.gateways.syketilfelle.FomTomPeriode
import no.nav.helse.flex.gateways.syketilfelle.SammeVentetidPeriode
import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleEksternClient
import no.nav.helse.flex.gateways.syketilfelle.VentetidForSykmeldingResponse
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultSyketilfelleDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.serialisertTilString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClientException
import java.time.LocalDate

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
    fun `burde returnere ventetid for sykmelding`() {
        var kaltPath: String? = null
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher { request ->
                kaltPath = request.url.encodedPath
                MockResponse
                    .Builder()
                    .body(
                        VentetidForSykmeldingResponse(
                            erUtenforVentetid = true,
                            periodeMedSammeVentetid =
                                listOf(
                                    SammeVentetidPeriode(
                                        ressursId = "sykmelding-1",
                                        ventetid = FomTomPeriode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-20")),
                                    ),
                                ),
                        ).serialisertTilString(),
                    ).addHeader("Content-Type", "application/json")
                    .build()
            }
        val response = syketilfelleEksternClient.getVentetidForSykmelding("sykmeldingId")
        response.erUtenforVentetid.shouldBeTrue()
        response.periodeMedSammeVentetid shouldHaveSize 1
        response.periodeMedSammeVentetid.first().ressursId shouldBe "sykmelding-1"
        kaltPath shouldBe "/api/bruker/v2/ventetid/sykmeldingId/ventetidForSykmelding"
    }

    @Test
    fun `burde kaste feil ved error ved henting av ventetidForSykmelding`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .code(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .addHeader("Content-Type", "application/json")
                    .build()
            }
        shouldThrow<RestClientException> {
            syketilfelleEksternClient.getVentetidForSykmelding("sykmeldingId")
        }
    }

    @Test
    fun `burde kaste feil ved tom body for ventetidForSykmelding`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .addHeader("Content-Type", "application/json")
                    .build()
            }
        shouldThrow<RuntimeException> {
            syketilfelleEksternClient.getVentetidForSykmelding("sykmeldingId")
        }
    }
}
