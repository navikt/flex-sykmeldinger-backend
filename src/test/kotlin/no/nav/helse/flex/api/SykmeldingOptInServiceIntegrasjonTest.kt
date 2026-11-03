package no.nav.helse.flex.api

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.defaultSykepengesoknadBackendDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.testdata.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus

class SykmeldingOptInServiceIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var sykmeldingOptInService: SykmeldingOptInService

    @Autowired
    lateinit var sykepengesoknadBackendMockWebServer: MockWebServer

    @BeforeEach
    fun lagreSykmelding() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_NAV,
                            brukerSvar = lagNaringsdrivendeBrukerSvar(),
                        ),
                    ),
            ),
        )
    }

    @AfterEach
    fun afterEach() {
        slettDatabase()
        sykepengesoknadBackendMockWebServer.dispatcher = defaultSykepengesoknadBackendDispatcher
    }

    @Test
    fun `burde lagre opt-in når kall til sykepengesoknad-backend lykkes`() {
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher { MockResponse.Builder().code(HttpStatus.OK.value()).build() }

        sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr"))

        optInDbRepository.findAllBySykmeldingId("1") shouldHaveSize 1
    }

    @Test
    fun `burde rulle tilbake opt-in når kall til sykepengesoknad-backend feiler`() {
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher { MockResponse.Builder().code(HttpStatus.INTERNAL_SERVER_ERROR.value()).build() }

        shouldThrow<Exception> {
            sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr"))
        }

        optInDbRepository.findAllBySykmeldingId("1").shouldBeEmpty()
    }
}
