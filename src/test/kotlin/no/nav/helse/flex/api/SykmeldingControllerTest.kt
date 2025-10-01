package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.arbeidsgiverdetaljer.domain.ArbeidsgiverDetaljer
import no.nav.helse.flex.gateways.syketilfelle.ErUtenforVentetidResponse
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SporsmalSvar
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SyketilfelleClientFake
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.testutils.tokenxToken
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should not be`
import org.amshove.kluent.`should not be null`
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

    @Autowired
    lateinit var syketilfelleClient: SyketilfelleClientFake

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
        fun `burde returnere brukerinfo som inneholder arbeidsgivere`() {
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

            val brukerinformasjon: BrukerinformasjonDTO = objectMapper.readValue(result)
            brukerinformasjon.arbeidsgivere.size `should be equal to` 1
        }

        @Test
        fun `burde returnere tom liste med arbeidsgivere i brukerinfo når perioden ikke overlapper`() {
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
        fun `burde returnere erOverSytti i brukerinfo`() {
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
            brukerinformasjon.erOverSyttiAar.`should be false`()
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
        fun `konverterer arbeidsgiverDetaljer dto riktig`() {
            val arbeidsgiverDetaljer =
                ArbeidsgiverDetaljer(
                    orgnummer = "orgnr",
                    juridiskOrgnummer = "jurorgnr",
                    navn = "Navn",
                    fom = LocalDate.parse("2021-01-01"),
                    tom = LocalDate.parse("2021-01-02"),
                    aktivtArbeidsforhold = true,
                    naermesteLeder = null,
                )
            val arbeidsgiverDetaljerDTO = arbeidsgiverDetaljer.konverterTilDto()
            arbeidsgiverDetaljerDTO `should be equal to`
                ArbeidsgiverDetaljerDTO(
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
                                lagSendSykmeldingRequestDTO(
                                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER,
                                    arbeidsgiverOrgnummer = "orgnummer",
                                    riktigNarmesteLeder = JaEllerNei.JA,
                                    harEgenmeldingsdager = JaEllerNei.JA,
                                ).serialisertTilString(),
                            ),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val returnertSykmelding: SykmeldingDTO = objectMapper.readValue(result)
            returnertSykmelding `should not be` null

            val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
            sykmelding?.sisteHendelse()?.status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
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
                                lagSendSykmeldingRequestDTO(arbeidsgiverOrgnummer = "orgnummer").serialisertTilString(),
                            ),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val returnertSykmelding: SykmeldingDTO = objectMapper.readValue(result)
            returnertSykmelding.arbeidsgiver `should be equal to`
                ArbeidsgiverDTO(
                    navn = "Arbeidsgivernavn",
                    stillingsprosent = 99,
                )
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
                                lagSendSykmeldingRequestDTO(
                                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG,
                                    arbeidsledig =
                                        ArbeidsledigDTO(
                                            arbeidsledigFraOrgnummer = null,
                                        ),
                                ).serialisertTilString(),
                            ),
                    ).andExpect(MockMvcResultMatchers.status().isOk)
                    .andReturn()
                    .response.contentAsString

            val returnertSykmelding: SykmeldingDTO = objectMapper.readValue(result)
            returnertSykmelding `should not be` null

            val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
            sykmelding?.sisteHendelse()?.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
        }

        @Test
        fun `burde få 404 når sykmeldingen ikke finnes`() =
            sjekkFår404NårSykmeldingenIkkeFinnes(
                content = lagSendSykmeldingRequestDTO().serialisertTilString(),
                HttpMethod.POST,
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/send" }

        @Test
        fun `burde feile dersom sykmelding har feil fnr`() =
            sjekkAtFeilerDersomSykmeldingHarFeilFnr(
                content = lagSendSykmeldingRequestDTO().serialisertTilString(),
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
            sykmelding?.sisteHendelse()?.status `should be equal to` HendelseStatus.AVBRUTT
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
                    validation = lagValidation(status = RuleType.INVALID),
                ),
            )

            val returnertSykmelding: SykmeldingDTO = kallChangeStatusEndepunkt(fnr = "fnr", content = SykmeldingChangeStatus.BEKREFT_AVVIST)
            returnertSykmelding `should not be` null

            val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
            sykmelding?.sisteHendelse()?.status `should be equal to` HendelseStatus.BEKREFTET_AVVIST
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

    @Nested
    inner class GetErUtenforVentetid {
        @AfterEach
        fun ryddOpp() {
            syketilfelleClient.reset()
        }

        @Test
        fun `burde hente svar på om sykmelding er utenfor ventetid`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            pasient = lagPasient(fnr = "fnr"),
                        ),
                ),
            )

            syketilfelleClient.setErUtenforVentetid(
                ErUtenforVentetidResponse(
                    erUtenforVentetid = true,
                    oppfolgingsdato = LocalDate.parse("2025-01-01"),
                ),
            )

            val result =
                mockMvc
                    .perform(
                        MockMvcRequestBuilders
                            .get("/api/v1/sykmeldinger/1/er-utenfor-ventetid")
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

            val erUtenforVentetidResponse: ErUtenforVentetidResponse = objectMapper.readValue(result)
            erUtenforVentetidResponse.`should not be null`()
        }

        @Test
        fun `burde få 404 når sykmeldingen ikke finnes`() =
            sjekkFår404NårSykmeldingenIkkeFinnes(
                content = lagSykmeldingGrunnlag().serialisertTilString(),
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/er-utenfor-ventetid" }

        @Test
        fun `burde feile dersom sykmelding har feil fnr`() =
            sjekkAtFeilerDersomSykmeldingHarFeilFnr(
                content = lagSykmeldingGrunnlag().serialisertTilString(),
            ) { sykmeldingId -> "/api/v1/sykmeldinger/$sykmeldingId/er-utenfor-ventetid" }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() =
            sjekkAtReturnereUnauthorizedNårViIkkeHarToken("/api/v1/sykmeldinger/1/er-utenfor-ventetid")

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() =
            sjekkAtReturnereUnauthorizedNårViHarFeilClaim("/api/v1/sykmeldinger/1/er-utenfor-ventetid")
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

fun lagSendSykmeldingRequestDTO(
    erOpplysningeneRiktige: JaEllerNei = JaEllerNei.JA,
    arbeidssituasjon: Arbeidssituasjon = Arbeidssituasjon.ANNET,
    arbeidsgiverOrgnummer: String? = null,
    harEgenmeldingsdager: JaEllerNei? = null,
    riktigNarmesteLeder: JaEllerNei? = null,
    arbeidsledig: ArbeidsledigDTO? = null,
    fiskerBladSvar: Blad? = null,
    fiskerBladSporsmaltekst: String = "Velg blad",
    fiskerLottOgHyreSvar: LottOgHyre? = null,
    fiskerLottOgHyreSporsmaltekst: String = "Mottar du lott eller er du på hyre?",
): SendSykmeldingRequestDTO =
    SendSykmeldingRequestDTO(
        erOpplysningeneRiktige =
            SporsmalSvar(
                "Stemmer opplysningene?",
                erOpplysningeneRiktige,
            ),
        arbeidssituasjon =
            SporsmalSvar(
                sporsmaltekst = "Jeg er sykmeldt som",
                svar = arbeidssituasjon,
            ),
        arbeidsgiverOrgnummer =
            arbeidsgiverOrgnummer?.let {
                SporsmalSvar(
                    sporsmaltekst = "Hva er arbeidsgiverens organisasjonsnummer?",
                    svar = it,
                )
            },
        riktigNarmesteLeder =
            riktigNarmesteLeder?.let {
                SporsmalSvar(
                    sporsmaltekst = "Er dette riktig nærmeste leder?",
                    svar = it,
                )
            },
        harEgenmeldingsdager =
            harEgenmeldingsdager?.let {
                SporsmalSvar(
                    sporsmaltekst = "Har du egenmeldingsdager?",
                    svar = it,
                )
            },
        arbeidsledig = arbeidsledig,
        egenmeldingsdager = null,
        egenmeldingsperioder = null,
        fisker =
            if (fiskerBladSvar != null && fiskerLottOgHyreSvar != null) {
                FiskerDTO(
                    blad =
                        SporsmalSvar(
                            sporsmaltekst = fiskerBladSporsmaltekst,
                            svar = fiskerBladSvar,
                        ),
                    lottOgHyre =
                        SporsmalSvar(
                            sporsmaltekst = fiskerLottOgHyreSporsmaltekst,
                            svar = fiskerLottOgHyreSvar,
                        ),
                )
            } else {
                null
            },
        harBruktEgenmelding = null,
        harForsikring = null,
        uriktigeOpplysninger = null,
    )
