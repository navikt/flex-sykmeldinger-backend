package no.nav.helse.flex.testconfig

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.EKSEMPEL_RESPONSE_FRA_EREG
import no.nav.helse.flex.clients.pdl.GraphQlRequest
import no.nav.helse.flex.clients.pdl.lagGetPersonResponseData
import no.nav.helse.flex.clients.pdl.lagGraphQlResponse
import no.nav.helse.flex.clients.pdl.lagHentIdenterResponseData
import no.nav.helse.flex.clients.syketilfelle.ErUtenforVentetidResponse
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import java.time.LocalDate
import kotlin.apply

fun simpleDispatcher(dispatcherFunc: (RecordedRequest) -> MockResponse): Dispatcher =
    object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = dispatcherFunc(request)
    }

fun singlePathDispatcher(
    path: String,
    dispatcherFunc: (RecordedRequest) -> MockResponse,
): Dispatcher =
    object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse =
            if (request.path == path) {
                dispatcherFunc(request)
            } else {
                MockResponse().setResponseCode(404)
            }
    }

val defaultAaregDispatcher =
    simpleDispatcher {
        when (it.path) {
            "/api/v2/arbeidstaker/arbeidsforholdoversikt" -> {
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(lagArbeidsforholdOversiktResponse(arbeidsforholdoversikter = emptyList()).serialisertTilString())
            }
            "/api/v2/arbeidssted/arbeidsforholdoversikt" -> {
                MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody(lagArbeidsforholdOversiktResponse(arbeidsforholdoversikter = emptyList()).serialisertTilString())
            }
            else -> {
                MockResponse().setResponseCode(404)
            }
        }
    }

val defaultEregDispatcher =
    simpleDispatcher {
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
    }

val defaultSyketilfelleDispatcher =
    simpleDispatcher {
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(
                ErUtenforVentetidResponse(
                    erUtenforVentetid = false,
                    oppfolgingsdato = LocalDate.parse("2025-01-01"),
                ).serialisertTilString(),
            )
    }

val defaultPdlDispatcher =
    simpleDispatcher { req ->
        val parsedReq = objectMapper.readValue<GraphQlRequest>(req.body.readByteArray())
        when (parsedReq.operationName) {
            "HentIdenterMedHistorikk" ->
                lagGraphQlResponse(
                    lagHentIdenterResponseData(),
                )
            "HentPersonNavn" ->
                lagGraphQlResponse(
                    lagGetPersonResponseData(),
                )
            "HentPersonFoedselsdato" ->
                lagGraphQlResponse(
                    lagGetPersonResponseData(),
                )
            else -> {
                MockResponse()
                    .setResponseCode(404)
                    .setHeader("Content-Type", "application/json")
            }
        }
    }

@TestConfiguration
class MockWebServereConfig {
    @Bean
    fun pdlMockWebServer() = pdlMockWebServer

    @Bean
    fun aaregMockWebServer() = aaregMockWebServer

    @Bean
    fun eregMockWebServer() = eregMockWebServer

    @Bean
    fun syketilfelleMockWebServer() = syketilfelleMockWebServer

    companion object {
        val logger = logger()

        init {
            logger.info("[TEST] Starter mock webservere")
        }

        val pdlMockWebServer =
            MockWebServer().apply {
                System.setProperty("PDL_BASE_URL", "http://localhost:$port")
                dispatcher = defaultPdlDispatcher
            }

        val aaregMockWebServer =
            MockWebServer().apply {
                System.setProperty("AAREG_URL", "http://localhost:$port")
                dispatcher = defaultAaregDispatcher
            }

        val eregMockWebServer =
            MockWebServer().apply {
                System.setProperty("EREG_URL", "http://localhost:$port")
                dispatcher = defaultEregDispatcher
            }

        val syketilfelleMockWebServer =
            MockWebServer().apply {
                System.setProperty("FLEX_SYKETILFELLE_URL", "http://localhost:$port")
                dispatcher = defaultSyketilfelleDispatcher
            }
    }
}
