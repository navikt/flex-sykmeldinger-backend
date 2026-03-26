package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.api.dto.FlexInternalSykmeldingDto
import no.nav.helse.flex.api.dto.MerknadtypeDTO
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagValidation
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should contain`
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class SykmeldingTexasControllerTest : FakesTestOppsett() {
    @Autowired
    private lateinit var environmentToggles: EnvironmentTogglesFake

    @BeforeAll
    fun setup() {
        environmentToggles.setEnvironment("prod")
    }

    @AfterEach
    fun ryddOpp() {
        slettDatabase()
    }

    @Nested
    inner class HentSykmeldingerKafka {
        val url = "/api/v1/sykmeldinger/kafka"

        @TestFactory
        fun tilgangskontroll(): List<DynamicTest?> {
            val sykmelding = sykmeldingRepository.save(lagSykmelding())
            return listOf(
                Triple("gyldig-token-role-sykepengesoknad-backend", HttpStatus.OK, "godtar riktig rolle"),
                Triple("gyldig-token-uten-rolle", HttpStatus.FORBIDDEN, "avviser token uten rolle"),
                Triple("gyldig-token-annen-rolle", HttpStatus.FORBIDDEN, "avviser token med annen rolle"),
                Triple("ikke-gyldig-token", HttpStatus.UNAUTHORIZED, "avviser ugyldig token"),
                Triple(null, HttpStatus.UNAUTHORIZED, "avviser manglende token"),
            ).map { (token, expectedStatus, name) ->
                DynamicTest.dynamicTest(name) {
                    sjekkStatus(
                        url = url,
                        token = token,
                        expectedStatus = expectedStatus,
                        content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId)),
                    )
                }
            }
        }

        @Test
        fun `burde returnere sykmelding i kafka format med siste pending merknad`() {
            val sykmelding = sykmeldingRepository.save(lagSykmelding(validation = lagValidation(RuleType.PENDING)))
            val respons =
                utførHentSykmeldingerMedKafkaFormat(
                    url = url,
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

        @Test
        fun `burde returnere sykmelding i kafka format uten merknader med ok validering`() {
            val sykmelding = sykmeldingRepository.save(lagSykmelding())
            val respons =
                utførHentSykmeldingerMedKafkaFormat(
                    url = url,
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

        @Test
        fun `burde returnere sykmelding i kafka format etter oppgitt dato`() {
            val sykmelding = sykmeldingRepository.save(lagSykmelding())
            val respons =
                utførHentSykmeldingerMedKafkaFormat(
                    url = url,
                    token = "gyldig-token-role-sykepengesoknad-backend",
                    expectedStatus = HttpStatus.OK,
                    content = SykmeldingerKafkaMessageRequest(listOf(sykmelding.sykmeldingId), sykmelding.tom),
                )

            respons.sykmeldinger.size `should be equal to` 1
            respons.sykmeldinger
                .first()
                .sykmelding.id `should be equal to` "1"
        }

        @Test
        fun `burde ikke returnere sykmelding i kafka format før oppgitt dato`() {
            val sykmelding = sykmeldingRepository.save(lagSykmelding())
            val respons =
                utførHentSykmeldingerMedKafkaFormat(
                    url = url,
                    token = "gyldig-token-role-sykepengesoknad-backend",
                    expectedStatus = HttpStatus.OK,
                    content =
                        SykmeldingerKafkaMessageRequest(
                            listOf(sykmelding.sykmeldingId),
                            sykmelding.tom.plusDays(1),
                        ),
                )

            respons.sykmeldinger `should be equal to` emptyList()
        }
    }

    @Nested
    inner class HentSykmeldingerForFlexInternal {
        private val url = "/api/v1/flex/sykmeldinger"
        private val fnr = "01010112345"
        private val annetFnr = "12345678901"

        @TestFactory
        fun tilgangskontroll() =
            listOf(
                Triple("gyldig-token-flex-gruppe", HttpStatus.OK, "godtar riktig gruppe"),
                Triple("gyldig-token-uten-gruppe", HttpStatus.FORBIDDEN, "avviser token uten gruppe"),
                Triple("gyldig-token-annen-gruppe", HttpStatus.FORBIDDEN, "avviser token med annen gruppe"),
                Triple("gyldig-token-role-sykepengesoknad-backend", HttpStatus.FORBIDDEN, "avviser sykepengesoknad-backend-rolle"),
                Triple("ikke-gyldig-token", HttpStatus.UNAUTHORIZED, "avviser ugyldig token"),
                Triple(null, HttpStatus.UNAUTHORIZED, "avviser manglende token"),
            ).map { (token, expectedStatus, name) ->
                DynamicTest.dynamicTest(name) {
                    sjekkStatus(url = url, token = token, expectedStatus = expectedStatus, content = FnrRequest(fnr))
                }
            }

        @Test
        fun `returnerer kun sykmeldinger for oppgitt fnr`() {
            sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(pasient = lagPasient(fnr = fnr))))
            sykmeldingRepository.save(
                lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", pasient = lagPasient(fnr = annetFnr))),
            )

            val sykmeldinger = postOgParseRespons(FnrRequest(fnr = fnr))

            sykmeldinger.size `should be equal to` 1
            sykmeldinger.map { it.id } `should contain` "1"
        }

        @Test
        fun `returnerer tom liste når ingen sykmeldinger finnes`() {
            postOgParseRespons(FnrRequest(fnr = fnr)).size `should be equal to` 0
        }

        @Test
        fun `returnerer alle sykmeldinger for fnr`() {
            repeat(3) { i ->
                sykmeldingRepository.save(
                    lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "$i", pasient = lagPasient(fnr = fnr))),
                )
            }

            postOgParseRespons(FnrRequest(fnr = fnr)).size `should be equal to` 3
        }

        @Test
        fun `produserer audit-logg med riktig navident og fnr`() {
            postOgParseRespons(FnrRequest(fnr = fnr))

            val entries = auditLogProducer.hentAuditEntries()
            entries.size `should be equal to` 1
            entries.first().utførtAv `should be equal to` "A123456"
            entries.first().oppslagPå `should be equal to` fnr
        }

        private fun postOgParseRespons(body: FnrRequest): List<FlexInternalSykmeldingDto> {
            val respons =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post(url)
                            .header("Authorization", "Bearer gyldig-token-flex-gruppe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body.serialisertTilString()),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response
                    .contentAsString
            return objectMapper.readValue<FlexInternalResponse>(respons).sykmeldinger
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
