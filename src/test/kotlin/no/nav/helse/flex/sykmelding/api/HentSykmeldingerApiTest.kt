package no.nav.helse.flex.sykmelding.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.FakesTestOppsett
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.jwt
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.sykmelding.api.dto.BrukerinformasjonDTO
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingDTO
import no.nav.helse.flex.sykmelding.domain.*
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDate

class HentSykmeldingerApiTest : FakesTestOppsett() {
    @AfterEach
    fun ryddOpp() {
        slettDatabase()
    }

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
                                    jwt(
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
        fun `burde ikke finne søknad som ikke finnes`() {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .get("/api/v1/sykmeldinger/0")
                        .header(
                            "Authorization",
                            "Bearer ${
                                jwt(
                                    fnr = "fnr",
                                )
                            }",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isNotFound)
        }

        @Test
        fun `burde ikke returnere søknad med feil fnr`() {
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
                                jwt(
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
                                jwt(
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
                                    jwt(
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
                                    jwt(
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
        fun `burde ikke returnere søknad med feil fnr`() {
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
                                    jwt(
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
                                jwt(
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
                                    jwt(
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
                                    jwt(
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
        fun `burde få 404 når sykmeldingen ikke finnes`() {
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
                                jwt(
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
                                jwt(
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
                                jwt(
                                    fnr = "fnr",
                                    acrClaim = "feil-claim",
                                )
                            }",
                        ).contentType(MediaType.APPLICATION_JSON),
                ).andExpect(MockMvcResultMatchers.status().isUnauthorized)
        }
    }
}
