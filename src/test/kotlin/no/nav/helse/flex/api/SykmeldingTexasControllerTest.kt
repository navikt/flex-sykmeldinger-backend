package no.nav.helse.flex.api

import no.nav.helse.flex.api.SykmeldingTexasController.SykmeldingerRequest
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.utils.serialisertTilString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class SykmeldingTexasControllerTest : FakesTestOppsett() {
    @AfterEach
    fun ryddOpp() {
        slettDatabase()
    }

    @Test
    fun `burde ha riktig tilgangskontroll`() {
        val sykmelding: Sykmelding =
            sykmeldingRepository.save(
                lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "id-1", pasient = lagPasient(fnr = "fnr"))),
            )
        "/api/v1/sykmeldinger/kafka".run {
            sjekkStatus(
                url = this,
                token = "gyldig-token-role-sykepengesoknad-backend",
                expectedStatus = HttpStatus.OK,
                content = SykmeldingerRequest(listOf(sykmelding.sykmeldingId)),
            )
            sjekkStatus(
                url = this,
                token = "gyldig-token-role-sykepengesoknad-backend",
                expectedStatus = HttpStatus.OK,
                content = SykmeldingerRequest(listOf(sykmelding.sykmeldingId)),
            )
            sjekkStatus(
                this,
                token = "gyldig-token-uten-rolle",
                expectedStatus = HttpStatus.FORBIDDEN,
                content = SykmeldingerRequest(listOf(sykmelding.sykmeldingId)),
            )
            sjekkStatus(
                this,
                token = "ikke-gyldig-token",
                expectedStatus = HttpStatus.UNAUTHORIZED,
                content = SykmeldingerRequest(listOf(sykmelding.sykmeldingId)),
            )
        }
    }

    fun sjekkStatus(
        url: String,
        httpMethod: HttpMethod = HttpMethod.POST,
        token: String,
        content: Any? = null,
        expectedStatus: HttpStatus = HttpStatus.OK,
    ) {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .request(httpMethod, url)
                    .authorizationHeader(token)
                    .apply {
                        if (content != null) {
                            this.contentType(MediaType.APPLICATION_JSON)
                            this.content(content.serialisertTilString())
                        }
                    },
            ).andExpect(MockMvcResultMatchers.status().`is`(expectedStatus.value()))
    }
}

private fun MockHttpServletRequestBuilder.authorizationHeader(token: String): MockHttpServletRequestBuilder =
    this.header("Authorization", "Bearer $token")
