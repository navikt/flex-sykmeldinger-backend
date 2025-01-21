package no.nav.helse.flex.sykmelding.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.jwt
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingDTO
import no.nav.helse.flex.sykmelding.domain.lagPasient
import no.nav.helse.flex.sykmelding.domain.lagSykmelding
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class HentSykmeldingerApiTest : FellesTestOppsett() {
    @AfterEach
    fun ryddOpp() {
        slettDatabase()
    }

    @Nested
    inner class HentSykmeldingEndepunkt {
        @Test
        fun `burde hente en sykmelding`() {
            sykemeldingRepository.save(
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
            sykemeldingRepository.save(
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
            sykemeldingRepository.save(
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
            sykemeldingRepository.save(
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
}
