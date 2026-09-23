package no.nav.helse.flex.api

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.defaultSykepengesoknadBackendDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.testdata.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.AnyException
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldThrow
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
            simpleDispatcher { MockResponse().setResponseCode(HttpStatus.OK.value()) }

        sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr"))

        optInDbRepository.findAllBySykmeldingId("1") shouldHaveSize 1
    }

    @Test
    fun `burde rulle tilbake opt-in når kall til sykepengesoknad-backend feiler`() {
        sykepengesoknadBackendMockWebServer.dispatcher =
            simpleDispatcher { MockResponse().setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value()) }

        invoking {
            sykmeldingOptInService.behandleOptIn(sykmeldingId = "1", identer = PersonIdenter("fnr"))
        } shouldThrow AnyException

        optInDbRepository.findAllBySykmeldingId("1").shouldBeEmpty()
    }
}
