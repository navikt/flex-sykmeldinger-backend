package no.nav.helse.flex.gateways

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.syketilfelle.ErUtenforVentetidResponse
import no.nav.helse.flex.gateways.syketilfelle.FomTomPeriode
import no.nav.helse.flex.gateways.syketilfelle.SammeVentetidPeriode
import no.nav.helse.flex.gateways.syketilfelle.SammeVentetidResponse
import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleEksternClient
import no.nav.helse.flex.gateways.syketilfelle.VentetidForSykmeldingResponse
import no.nav.helse.flex.testconfig.RestClientOppsett
import no.nav.helse.flex.testconfig.defaultSyketilfelleDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be true`
import org.amshove.kluent.`should throw`
import org.amshove.kluent.shouldHaveSize
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
    fun `burde returnere svar på om sykmelding er utenfor ventetid`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(
                        ErUtenforVentetidResponse(
                            erUtenforVentetid = true,
                        ).serialisertTilString(),
                    ).addHeader("Content-Type", "application/json")
            }
        val erUtenforVentetidResponse = syketilfelleEksternClient.getErUtenforVentetid(PersonIdenter("fnr"), "sykmeldingId")
        erUtenforVentetidResponse.erUtenforVentetid.`should be true`()
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

    @Test
    fun `burde returnere perioder med samme ventetid`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(
                        SammeVentetidResponse(
                            ventetidPerioder =
                                listOf(
                                    SammeVentetidPeriode(
                                        ressursId = "sykmelding-1",
                                        ventetid = FomTomPeriode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-20")),
                                    ),
                                ),
                        ).serialisertTilString(),
                    ).addHeader("Content-Type", "application/json")
            }
        val response = syketilfelleEksternClient.getPerioderMedSammeVentetid("sykmeldingId")
        response.ventetidPerioder shouldHaveSize 1
        response.ventetidPerioder.first().ressursId `should be equal to` "sykmelding-1"
    }

    @Test
    fun `burde kaste feil ved error ved henting av perioderMedSammeVentetid`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .addHeader("Content-Type", "application/json")
            }
        invoking {
            syketilfelleEksternClient.getPerioderMedSammeVentetid("sykmeldingId")
        } `should throw` RestClientException::class
    }

    @Test
    fun `burde kaste feil ved tom body for perioderMedSammeVentetid`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .addHeader("Content-Type", "application/json")
            }
        invoking {
            syketilfelleEksternClient.getPerioderMedSammeVentetid("sykmeldingId")
        } `should throw` RuntimeException::class
    }

    @Test
    fun `burde returnere ventetid for sykmelding`() {
        var kaltPath: String? = null
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher { request ->
                kaltPath = request.requestUrl?.encodedPath
                MockResponse()
                    .setBody(
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
            }
        val response = syketilfelleEksternClient.getVentetidForSykmelding("sykmeldingId")
        response.erUtenforVentetid.`should be true`()
        response.periodeMedSammeVentetid shouldHaveSize 1
        response.periodeMedSammeVentetid.first().ressursId `should be equal to` "sykmelding-1"
        kaltPath `should be equal to` "/api/bruker/v2/ventetid/sykmeldingId/ventetidForSykmelding"
    }

    @Test
    fun `burde kaste feil ved error ved henting av ventetidForSykmelding`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .addHeader("Content-Type", "application/json")
            }
        invoking {
            syketilfelleEksternClient.getVentetidForSykmelding("sykmeldingId")
        } `should throw` RestClientException::class
    }

    @Test
    fun `burde kaste feil ved tom body for ventetidForSykmelding`() {
        syketilfelleMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .addHeader("Content-Type", "application/json")
            }
        invoking {
            syketilfelleEksternClient.getVentetidForSykmelding("sykmeldingId")
        } `should throw` RuntimeException::class
    }
}
