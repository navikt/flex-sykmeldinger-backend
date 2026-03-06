package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.`should be equal to`
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
        val sykmelding = sykmeldingRepository.save(lagSykmelding())
        "/api/v1/sykmeldinger/kafka".run {
            sjekkStatus(
                url = this,
                token = "gyldig-token-role-sykepengesoknad-backend",
                expectedStatus = HttpStatus.OK,
                content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
            )
            sjekkStatus(
                url = this,
                token = "gyldig-token-role-sykepengesoknad-backend",
                expectedStatus = HttpStatus.OK,
                content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
            )
            sjekkStatus(
                this,
                token = "gyldig-token-uten-rolle",
                expectedStatus = HttpStatus.FORBIDDEN,
                content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
            )
            sjekkStatus(
                this,
                token = "ikke-gyldig-token",
                expectedStatus = HttpStatus.UNAUTHORIZED,
                content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
            )
        }
    }

    @Test
    fun `burde returnere sykmelding i kafka format`() {
        val sykmelding = sykmeldingRepository.save(lagSykmelding())
        "/api/v1/sykmeldinger/kafka".run {
            sjekkAtViReturnererSykmeldingKafka(
                url = this,
                token = "gyldig-token-role-sykepengesoknad-backend",
                expectedStatus = HttpStatus.OK,
                content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
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

    fun sjekkAtViReturnererSykmeldingKafka(
        url: String,
        httpMethod: HttpMethod = HttpMethod.POST,
        token: String,
        content: SykmeldingerKafkaMessageRequest? = null,
        expectedStatus: HttpStatus = HttpStatus.OK,
    ) {
        val respons =
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
                .andReturn()
                .response
                .contentAsString

        val responsVerdi: SykmeldingKafkaMessageResponse = objectMapper.readValue(respons)

        responsVerdi.sykmeldinger.size `should be equal to` 1
        responsVerdi.sykmeldinger
            .first()
            .sykmelding.id `should be equal to` "1"
    }
}

private fun MockHttpServletRequestBuilder.authorizationHeader(token: String): MockHttpServletRequestBuilder =
    this.header("Authorization", "Bearer $token")
