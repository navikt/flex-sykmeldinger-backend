package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagAktivitetIkkeMulig
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testutils.tokenxToken
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import no.nav.helse.flex.virksomhet.domain.Virksomhet
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be`
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate

class SykmeldingControllerTest : FakesTestOppsett() {
    @AfterEach
    fun ryddOpp() {
        slettDatabase()
    }

    @Autowired
    lateinit var oauth2Server: MockOAuth2Server

    @Nested
    inner class HentSykmeldingEndepunkt {
        @Test
        fun `burde hente en sykmelding`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )

            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .get("/api/v1/sykmeldinger/1")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val sykmelding: SykmeldingDTO = objectMapper.readValue(result)

            sykmelding.id `should be equal to` "1"
        }

        @Test
        fun `burde få 404 når sykmeldingen ikke finnes`() =
            sjekkFår404NårSykmeldingenIkkeFinnes(
                content = lagSykmeldingGrunnlag().serialisertTilString(),
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId" }

        @Test
        fun `burde feile dersom sykmelding har feil fnr`() =
            sjekkAtFeilerDersomSykmeldingHarFeilFnr(
                content = lagSykmeldingGrunnlag().serialisertTilString(),
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId" }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() = sjekkAtReturnereUnauthorizedNårViIkkeHarToken("/api/v1/sykmeldinger/1")

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() = sjekkAtReturnereUnauthorizedNårViHarFeilClaim("/api/v1/sykmeldinger/1")
    }

    @Nested
    inner class HentAlleSykmeldingerEndepunkt {
        @Test
        fun `burde hente én sykmelding`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )

            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .get("/api/v1/sykmeldinger")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val sykmeldinger: List<SykmeldingDTO> = objectMapper.readValue(result)
            sykmeldinger.size `should be equal to` 1
            val sykmelding = sykmeldinger[0]
            sykmelding.id `should be equal to` "1"
            sykmelding.pasient.fnr `should be equal to` "fnr"
        }

        @Test
        fun `burde returnere tom liste om ingen sykmeldinger finnes`() {
            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .get("/api/v1/sykmeldinger")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val sykmeldinger: List<SykmeldingDTO> = objectMapper.readValue(result)
            sykmeldinger.size `should be equal to` 0
        }

        @Test
        fun `burde ikke returnere sykmeldinger med feil fnr`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )
            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .get("/api/v1/sykmeldinger")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "feil_fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val sykmeldinger: List<SykmeldingDTO> = objectMapper.readValue(result)
            sykmeldinger.size `should be equal to` 0
        }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() = sjekkAtReturnereUnauthorizedNårViIkkeHarToken("/api/v1/sykmeldinger")

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() = sjekkAtReturnereUnauthorizedNårViHarFeilClaim("/api/v1/sykmeldinger")
    }

    @Nested
    inner class HentBrukerInfoEndepunkt {
        @Test
        fun `burde hente brukerinfo`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                            aktiviteter =
                                listOf(
                                    lagAktivitetIkkeMulig(
                                        fom = LocalDate.parse("2021-01-01"),
                                        tom = LocalDate.parse("2021-01-10"),
                                    ),
                                ),
                        ),
                ),
            )

            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "fnr",
                    fom = LocalDate.parse("2021-01-01"),
                ),
            )

            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .get("/api/v1/sykmeldinger/1/brukerinformasjon")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val brukerinformasjon: no.nav.helse.flex.api.dto.BrukerinformasjonDTO = objectMapper.readValue(result)
            brukerinformasjon.arbeidsgivere.size `should be equal to` 1
        }

        @Test
        fun `burde ikke hente brukerinfo når perioden ikke overlapper`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                            aktiviteter =
                                listOf(
                                    lagAktivitetIkkeMulig(
                                        fom = LocalDate.parse("2022-01-01"),
                                        tom = LocalDate.parse("2022-01-10"),
                                    ),
                                ),
                        ),
                ),
            )

            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "fnr",
                    orgnummer = "orgnummer",
                    fom = LocalDate.parse("2021-01-01"),
                    tom = LocalDate.parse("2021-01-09"),
                ),
            )

            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .get("/api/v1/sykmeldinger/1/brukerinformasjon")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val brukerinformasjon: BrukerinformasjonDTO = objectMapper.readValue(result)
            brukerinformasjon.arbeidsgivere.size `should be equal to` 0
        }

        @Test
        fun `burde få 404 når sykmeldingen ikke finnes, selv om arbeidsforhold finnes`() {
            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "fnr",
                    orgnummer = "orgnummer",
                    fom = LocalDate.parse("2021-01-01"),
                    tom = LocalDate.parse("2021-01-09"),
                ),
            )

            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger/1/brukerinformasjon")
                        .header(
                            "Authorization",
                            "Bearer ${
                                oauth2Server.tokenxToken(
                                    fnr = "fnr",
                                )
                            }",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isNotFound)
        }

        @Test
        fun `burde få 404 når sykmeldingen ikke finnes`() =
            sjekkFår404NårSykmeldingenIkkeFinnes(
                content = lagSykmeldingGrunnlag().serialisertTilString(),
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/brukerinformasjon" }

        @Test
        fun `burde feile dersom sykmelding har feil fnr`() =
            sjekkAtFeilerDersomSykmeldingHarFeilFnr(
                content = lagSykmeldingGrunnlag().serialisertTilString(),
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/brukerinformasjon" }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() =
            sjekkAtReturnereUnauthorizedNårViIkkeHarToken("/api/v1/sykmeldinger/1/brukerinformasjon")

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() =
            sjekkAtReturnereUnauthorizedNårViHarFeilClaim("/api/v1/sykmeldinger/1/brukerinformasjon")
    }

    @Nested
    inner class ExtensionFuncs {
        @Test
        fun `konverterer virksomhet dto riktig`() {
            val virksomhet =
                Virksomhet(
                    orgnummer = "orgnr",
                    juridiskOrgnummer = "jurorgnr",
                    navn = "Navn",
                    fom = LocalDate.parse("2021-01-01"),
                    tom = LocalDate.parse("2021-01-02"),
                    aktivtArbeidsforhold = true,
                    naermesteLeder = null,
                )
            val virksomhetDTO = virksomhet.konverterTilDto()
            virksomhetDTO `should be equal to`
                VirksomhetDTO(
                    orgnummer = "orgnr",
                    juridiskOrgnummer = "jurorgnr",
                    navn = "Navn",
                    aktivtArbeidsforhold = true,
                    naermesteLeder = null,
                )
        }

        @Test
        fun `burde konvertere narmeste leder dto riktig`() {
            val narmesteLeder =
                lagNarmesteLeder(
                    narmesteLederNavn = "Navn",
                    orgnummer = "orgnr",
                )
            val narmesteLederDTO = narmesteLeder.konverterTilDto()
            narmesteLederDTO `should be equal to`
                NarmesteLederDTO(
                    navn = "Navn",
                    orgnummer = "orgnr",
                )
        }
    }

    @Nested
    inner class SendSykmeldingEndepunkt {
        @Test
        fun `burde sende sykmelding for arbeidstaker til arbeidsgiver`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )

            // val sykmeldingSporsmalSvarDto = lagSykmeldingSporsmalSvarDto()

            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/api/v1/sykmeldinger/1/send")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON)
                            .content(
                                lagSendBody().serialisertTilString(),
                            ),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val returnertSykmelding: SykmeldingDTO = objectMapper.readValue(result)
            returnertSykmelding `should not be` null

            val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
            sykmelding?.sisteStatus()?.status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
        }

        @Test
        fun `burde inkludere arbeidsgiver info i sendt sykmelding`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )

            arbeidsforholdRepository.save(
                lagArbeidsforhold(orgnummer = "orgnummer", fnr = "fnr"),
            )

            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/api/v1/sykmeldinger/1/send")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON)
                            .content(
                                lagSendBody(arbeidsgiverOrgnummer = "orgnummer").serialisertTilString(),
                            ),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val returnertSykmelding: SykmeldingDTO = objectMapper.readValue(result)
            returnertSykmelding.arbeidsgiver.shouldNotBeNull()
        }

        @Test
        fun `burde sende sykmelding for arbeidsledig til nav`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )

            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/api/v1/sykmeldinger/1/send")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = "fnr",
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON)
                            .content(
                                lagSendBody(
                                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG,
                                    arbeidsledig =
                                        Arbeidsledig(
                                            arbeidsledigFraOrgnummer = "orgnummer",
                                        ),
                                ).serialisertTilString(),
                            ),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val returnertSykmelding: SykmeldingDTO = objectMapper.readValue(result)
            returnertSykmelding `should not be` null

            val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
            sykmelding?.sisteStatus()?.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
        }

        @Test
        fun `burde få 404 når sykmeldingen ikke finnes`() =
            sjekkFår404NårSykmeldingenIkkeFinnes(
                content = lagSendBody().serialisertTilString(),
                HttpMethod.POST,
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/send" }

        @Test
        fun `burde feile dersom sykmelding har feil fnr`() =
            sjekkAtFeilerDersomSykmeldingHarFeilFnr(
                content = lagSendBody().serialisertTilString(),
                HttpMethod.POST,
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/send" }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() =
            sjekkAtReturnereUnauthorizedNårViIkkeHarToken("/api/v1/sykmeldinger/1/send", HttpMethod.POST)

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() =
            sjekkAtReturnereUnauthorizedNårViHarFeilClaim("/api/v1/sykmeldinger/1/send", HttpMethod.POST)
    }

    @Nested
    inner class ChangeStatusSykmeldingEndepunkt {
        @Test
        fun `burde endre status til AVBRUTT`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )

            val returnertSykmelding: SykmeldingDTO = kallChangeStatusEndepunkt(fnr = "fnr", content = SykmeldingChangeStatus.AVBRYT)
            returnertSykmelding `should not be` null

            val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
            sykmelding?.sisteStatus()?.status `should be equal to` HendelseStatus.AVBRUTT
        }

        @Test
        fun `burde endre status til BEKREFTET_AVVIST`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )

            val returnertSykmelding: SykmeldingDTO = kallChangeStatusEndepunkt(fnr = "fnr", content = SykmeldingChangeStatus.BEKREFT_AVVIST)
            returnertSykmelding `should not be` null

            val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
            sykmelding?.sisteStatus()?.status `should be equal to` HendelseStatus.BEKREFTET_AVVIST
        }

        @Test
        fun `burde få 404 når sykmeldingen ikke finnes`() =
            sjekkFår404NårSykmeldingenIkkeFinnes(
                content = SykmeldingChangeStatus.AVBRYT.serialisertTilString(),
                HttpMethod.POST,
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/change-status" }

        @Test
        fun `burde feile dersom sykmelding har feil fnr`() =
            sjekkAtFeilerDersomSykmeldingHarFeilFnr(
                content = SykmeldingChangeStatus.AVBRYT.serialisertTilString(),
                HttpMethod.POST,
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/change-status" }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() =
            sjekkAtReturnereUnauthorizedNårViIkkeHarToken("/api/v1/sykmeldinger/1/change-status", HttpMethod.POST)

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() =
            sjekkAtReturnereUnauthorizedNårViHarFeilClaim("/api/v1/sykmeldinger/1/change-status", HttpMethod.POST)

        private fun kallChangeStatusEndepunkt(
            fnr: String,
            content: SykmeldingChangeStatus,
        ): SykmeldingDTO {
            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .post("/api/v1/sykmeldinger/1/change-status")
                            .header(
                                "Authorization",
                                "Bearer ${
                                    oauth2Server.tokenxToken(
                                        fnr = fnr,
                                    )
                                }",
                            ).contentType(MediaType.APPLICATION_JSON)
                            .content(
                                content.serialisertTilString(),
                            ),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val returnertSykmelding: SykmeldingDTO = objectMapper.readValue(result)
            return returnertSykmelding
        }
    }

    fun sjekkFår404NårSykmeldingenIkkeFinnes(
        content: String = "{}",
        httpMethod: HttpMethod = HttpMethod.GET,
        urlProducer: (sykmeldingId: String) -> String,
    ) {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .request(httpMethod, urlProducer("1"))
                    .header(
                        "Authorization",
                        "Bearer ${
                            oauth2Server.tokenxToken(
                                fnr = "fnr",
                            )
                        }",
                    ).content(content)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isNotFound)
    }

    fun sjekkAtFeilerDersomSykmeldingHarFeilFnr(
        content: String,
        httpMethod: HttpMethod = HttpMethod.GET,
        urlProducer: (sykmeldingId: String) -> String,
    ) {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id = "1",
                        pasient = lagPasient(fnr = "fnr"),
                    ),
            ),
        )
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .request(httpMethod, urlProducer("1"))
                    .header(
                        "Authorization",
                        "Bearer ${
                            oauth2Server.tokenxToken(
                                fnr = "feil_fnr",
                            )
                        }",
                    ).content(content)
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }

    fun sjekkAtReturnereUnauthorizedNårViIkkeHarToken(
        url: String,
        httpMethod: HttpMethod = HttpMethod.GET,
    ) {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .request(httpMethod, url)
                    .content("{}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }

    fun sjekkAtReturnereUnauthorizedNårViHarFeilClaim(
        url: String,
        httpMethod: HttpMethod = HttpMethod.GET,
    ) {
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .request(httpMethod, url)
                    .header(
                        "Authorization",
                        "Bearer ${
                            oauth2Server.tokenxToken(
                                fnr = "fnr",
                                acrClaim = "feil-claim",
                            )
                        }",
                    ).content("{}")
                    .contentType(MediaType.APPLICATION_JSON),
            ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
    }
}

fun lagSendBody(
    arbeidssituasjon: Arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER,
    arbeidsgiverOrgnummer: String? = null,
    arbeidsledig: Arbeidsledig? = null,
): SendBody =
    SendBody(
        erOpplysningeneRiktige = "YES",
        arbeidssituasjon = arbeidssituasjon,
        arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
        riktigNarmesteLeder = null,
        harEgenmeldingsdager = "NO",
        arbeidsledig = arbeidsledig,
    )
