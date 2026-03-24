package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.api.dto.MerknadtypeDTO
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagValidation
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
                token = "gyldig-token-annen-rolle",
                expectedStatus = HttpStatus.FORBIDDEN,
                content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
            )
            sjekkStatus(
                this,
                token = "ikke-gyldig-token",
                expectedStatus = HttpStatus.UNAUTHORIZED,
                content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
            )
            sjekkStatus(
                this,
                token = null,
                expectedStatus = HttpStatus.UNAUTHORIZED,
                content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
            )
        }
    }

    @Test
    fun `burde returnere sykmelding i kafka format med siste pending merknad`() {
        val sykmelding = sykmeldingRepository.save(lagSykmelding(validation = lagValidation(RuleType.PENDING)))
        "/api/v1/sykmeldinger/kafka".run {
            val respons =
                utførHentSykmeldingerMedKafkaFormat(
                    url = this,
                    token = "gyldig-token-role-sykepengesoknad-backend",
                    expectedStatus = HttpStatus.OK,
                    content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
                )

            respons.sykmeldinger.size `should be equal to` 1
            respons.sykmeldinger
                .first()
                .run {
                    this.sykmelding.id `should be equal to` "1"
                    this.sykmelding.merknader!!.size `should be equal to` 1
                    this.sykmelding.merknader
                        .first()
                        .type `should be equal to` MerknadtypeDTO.UNDER_BEHANDLING
                }
        }
    }

    @Test
    fun `burde returnere sykmelding i kafka format uten merknader med ok validering`() {
        val sykmelding = sykmeldingRepository.save(lagSykmelding())
        "/api/v1/sykmeldinger/kafka".run {
            val respons =
                utførHentSykmeldingerMedKafkaFormat(
                    url = this,
                    token = "gyldig-token-role-sykepengesoknad-backend",
                    expectedStatus = HttpStatus.OK,
                    content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
                )

            respons.sykmeldinger.size `should be equal to` 1
            respons.sykmeldinger
                .first()
                .run {
                    this.sykmelding.id `should be equal to` "1"
                    this.sykmelding.merknader `should be equal to` null
                }
        }
    }

    @Test
    fun `burde returnere sykmelding i kafka format etter oppgitt dato`() {
        val sykmelding = sykmeldingRepository.save(lagSykmelding())
        "/api/v1/sykmeldinger/kafka".run {
            val respons =
                utførHentSykmeldingerMedKafkaFormat(
                    url = this,
                    token = "gyldig-token-role-sykepengesoknad-backend",
                    expectedStatus = HttpStatus.OK,
                    content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId), sykmelding.tom),
                )

            respons.sykmeldinger.size `should be equal to` 1
            respons.sykmeldinger
                .first()
                .sykmelding.id `should be equal to` "1"
        }
    }

    @Test
    fun `burde ikke returnere sykmelding i kafka format før oppgitt dato`() {
        val sykmelding = sykmeldingRepository.save(lagSykmelding())
        "/api/v1/sykmeldinger/kafka".run {
            val respons =
                utførHentSykmeldingerMedKafkaFormat(
                    url = this,
                    token = "gyldig-token-role-sykepengesoknad-backend",
                    expectedStatus = HttpStatus.OK,
                    content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId), sykmelding.tom.plusDays(1)),
                )

            respons.sykmeldinger `should be equal to` emptyList()
        }
    }

    fun sjekkStatus(
        url: String,
        httpMethod: HttpMethod = HttpMethod.POST,
        token: String?,
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

    fun utførHentSykmeldingerMedKafkaFormat(
        url: String,
        httpMethod: HttpMethod = HttpMethod.POST,
        token: String,
        content: SykmeldingerKafkaMessageRequest? = null,
        expectedStatus: HttpStatus = HttpStatus.OK,
    ): SykmeldingKafkaMessageResponse {
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

        return objectMapper.readValue(respons)
    }
}

private fun MockHttpServletRequestBuilder.authorizationHeader(token: String?): MockHttpServletRequestBuilder =
    this.header("Authorization", "Bearer $token")
