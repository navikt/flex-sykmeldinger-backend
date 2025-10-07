package no.nav.helse.flex.testconfig

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.gateways.EKSEMPEL_RESPONSE_FRA_EREG
import no.nav.helse.flex.gateways.pdl.GraphQlRequest
import no.nav.helse.flex.gateways.pdl.lagGetPersonResponseData
import no.nav.helse.flex.gateways.pdl.lagGraphQlResponse
import no.nav.helse.flex.gateways.pdl.lagHentIdenterResponseData
import no.nav.helse.flex.gateways.syketilfelle.ErUtenforVentetidResponse
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

val defaultAaregDispatcher =
    simpleDispatcher {
        MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody(lagArbeidsforholdOversiktResponse(arbeidsforholdoversikter = emptyList()).serialisertTilString())
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
                dispatcher = defaultPdlDispatcher
            }

        val aaregMockWebServer =
            MockWebServer().apply {
                dispatcher = defaultAaregDispatcher
            }

        val eregMockWebServer =
            MockWebServer().apply {
                dispatcher = defaultEregDispatcher
            }

        val syketilfelleMockWebServer =
            MockWebServer().apply {
                dispatcher = defaultSyketilfelleDispatcher
            }

        init {
            System.setProperty("PDL_BASE_URL", "http://localhost:${pdlMockWebServer.port}")
            System.setProperty("AAREG_URL", "http://localhost:${aaregMockWebServer.port}")
            System.setProperty("EREG_URL", "http://localhost:${eregMockWebServer.port}")
            System.setProperty("FLEX_SYKETILFELLE_URL", "http://localhost:${syketilfelleMockWebServer.port}")
        }
    }
}
