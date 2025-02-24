package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.EKSEMPEL_RESPONSE_FRA_EREG
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.lagPasient
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import no.nav.helse.flex.sykmelding.logikk.SykmeldingLagrer
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.defaultEregDispatcher
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingLagrerFakeTest : FakesTestOppsett() {
    @Autowired
    private lateinit var sykmeldingLagrer: SykmeldingLagrer

    @Autowired
    private lateinit var eregMockWebServer: MockWebServer

    @Autowired
    private lateinit var aaregClient: AaregClientFake

    @AfterEach
    fun tearDown() {
        slettDatabase()
        eregMockWebServer.dispatcher = defaultEregDispatcher
        aaregClient.reset()
    }

    @Test
    fun `burde lagre sykmelding`() {
        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            ),
        )

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
    }

    @Test
    fun `burde deduplisere sykmeldinger`() {
        repeat(2) {
            sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(
                SykmeldingMedBehandlingsutfallMelding(
                    sykmelding = lagSykmeldingGrunnlag(id = "1"),
                    validation = lagValidation(),
                ),
            )
        }

        sykmeldingRepository.findAll().size `should be equal to` 1
    }

    @Test
    fun `burde sette status til ny`() {
        val sykmeldingMedBehandlingsutfall =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )
        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)

        val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
        sykmelding.shouldNotBeNull()
        sykmelding.statuser.size `should be equal to` 1
        val status = sykmelding.statuser[0]
        status.status `should be equal to` HendelseStatus.APEN
    }

    @Test
    fun `burde hente arbeidsforhold nar sykmelding lagres`() {
        aaregClient.setArbeidsforholdoversikt(lagArbeidsforholdOversiktResponse(listOf(lagArbeidsforholdOversikt(fnr = "fnr"))), "fnr")

        eregMockWebServer.dispatcher =
            simpleDispatcher {
                lagJsonResponse(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
            }

        val sykmeldingMedBehandlingsutfall =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr")),
                validation = lagValidation(),
            )

        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)

        val arbeidsforhold = arbeidsforholdRepository.getAllByFnr("fnr")
        arbeidsforhold.size `should be equal to` 1
    }
}

private fun lagJsonResponse(body: String) =
    MockResponse()
        .setBody(body)
        .setHeader("Content-Type", "application/json")
