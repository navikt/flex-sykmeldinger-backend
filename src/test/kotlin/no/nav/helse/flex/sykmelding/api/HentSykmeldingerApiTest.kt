package no.nav.helse.flex.sykmelding.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.FakesTestOppsett
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.sykmelding.api.dto.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.tokenxToken
import no.nav.helse.flex.virksomhet.domain.Virksomhet
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be`
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate

class HentSykmeldingerApiTest : FakesTestOppsett() {
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
        fun `burde ikke finne sykmelding som ikke finnes`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger/0")
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
        fun `burde ikke returnere sykmelding med feil fnr`() {
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
                        .get("/api/v1/sykmeldinger/1")
                        .header(
                            "Authorization",
                            "Bearer ${
                                oauth2Server.tokenxToken(
                                    fnr = "feil_fnr",
                                )
                            }",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isForbidden)
        }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger/1")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger/1")
                        .header(
                            "Authorization",
                            "Bearer ${
                                oauth2Server.tokenxToken(
                                    fnr = "fnr",
                                    acrClaim = "feil-claim",
                                )
                            }",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }
    }

    @Nested
    inner class HentAlleSykmeldingerEndepunkt {
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
        fun `burde returnere unauthorized når vi ikke har token`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger")
                        .header(
                            "Authorization",
                            "Bearer ${
                                oauth2Server.tokenxToken(
                                    fnr = "fnr",
                                    acrClaim = "feil-claim",
                                )
                            }",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }
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

            val brukerinformasjon: BrukerinformasjonDTO = objectMapper.readValue(result)
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
        fun `burde ikke returnere brukerinfo med feil fnr`() {
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
                        .get("/api/v1/sykmeldinger/1/brukerinformasjon")
                        .header(
                            "Authorization",
                            "Bearer ${
                                oauth2Server.tokenxToken(
                                    fnr = "feil_fnr",
                                )
                            }",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isForbidden)
        }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger/1/brukerinformasjon")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger/1/brukerinformasjon")
                        .header(
                            "Authorization",
                            "Bearer ${
                                oauth2Server.tokenxToken(
                                    fnr = "fnr",
                                    acrClaim = "feil-claim",
                                )
                            }",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }
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
        fun `burde sende sykmelding`() {
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
            sykmelding?.sisteStatus()?.status `should be equal to` HendelseStatus.SENDT
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
        fun `burde få 404 når sykmeldingen ikke finnes`() {
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
                        ).content(lagSendBody().serialisertTilString())
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isNotFound)
        }

        @Test
        fun `burde feile dersom sykmelding har feil fnr`() {
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
                        .post("/api/v1/sykmeldinger/1/send")
                        .header(
                            "Authorization",
                            "Bearer ${
                                oauth2Server.tokenxToken(
                                    fnr = "feil_fnr",
                                )
                            }",
                        ).content(lagSendBody().serialisertTilString())
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isForbidden)
        }

        @Test
        fun `burde returnere unauthorized når vi ikke har token`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/sykmeldinger/1/send")
                        .content("{}")
                        .contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }

        @Test
        fun `burde returnere unauthorized når vi har feil claim`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/sykmeldinger/1/send")
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
}

fun lagSendBody(arbeidsgiverOrgnummer: String? = null): SendBody {
    val sendBody: SendBody =
        objectMapper.readValue(
            """
        {"erOpplysningeneRiktige":"YES","arbeidssituasjon":"ARBEIDSTAKER",
        "arbeidsgiverOrgnummer":null,"riktigNarmesteLeder":null,"harEgenmeldingsdager":"NO"}
        """,
        )
    return sendBody.copy(arbeidsgiverOrgnummer = arbeidsgiverOrgnummer)
}
