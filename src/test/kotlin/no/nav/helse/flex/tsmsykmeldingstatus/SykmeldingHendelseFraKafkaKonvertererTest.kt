package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.NowFactoryFake
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.testdata.lagBrukerSvarKafkaDto
import no.nav.helse.flex.tsmsykmeldingstatus.dto.*
import org.amshove.kluent.*
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.ZoneOffset

class SykmeldingHendelseFraKafkaKonvertererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingHendelseFraKafkaKonverterer: SykmeldingHendelseFraKafkaKonverterer

    @Autowired
    lateinit var nowFactory: NowFactoryFake

    @AfterEach
    fun afterEach() {
        nowFactory.reset()
    }

    @Test
    fun `burde konvertere status til sykmelding hendelse`() {
        val status =
            lagSykmeldingStatusKafkaDTO(
                statusEvent = "APEN",
                timestamp = Instant.parse("2021-01-01T00:00:00Z").atOffset(ZoneOffset.UTC),
                brukerSvarKafkaDTO = lagBrukerSvarKafkaDto(ArbeidssituasjonDTO.ARBEIDSTAKER),
            )

        sykmeldingHendelseFraKafkaKonverterer
            .konverterSykmeldingHendelseFraKafkaDTO(
                status = status,
                source = "TEST_SOURCE",
            ).run {
                this.status `should be equal to` HendelseStatus.APEN
                hendelseOpprettet `should be equal to` Instant.parse("2021-01-01T00:00:00Z")
                source `should be equal to` "TEST_SOURCE"
                brukerSvar.`should not be null`()
            }
    }

    @Test
    fun `burde sette lokaltOpprettet til nå`() {
        nowFactory.setNow(Instant.parse("2021-01-01T00:00:00Z"))

        val status = lagSykmeldingStatusKafkaDTO()

        val hendelse = sykmeldingHendelseFraKafkaKonverterer.konverterSykmeldingHendelseFraKafkaDTO(status = status)
        hendelse.lokaltOpprettet `should be equal to` Instant.parse("2021-01-01T00:00:00Z")
    }

    @ParameterizedTest
    @ValueSource(strings = ["SENDT", "BEKREFTET"])
    fun `burde feile uten brukerSvar når `(statusEvent: String) {
        val status =
            lagSykmeldingStatusKafkaDTO(
                statusEvent = statusEvent,
                brukerSvarKafkaDTO = null,
            )

        invoking { sykmeldingHendelseFraKafkaKonverterer.konverterSykmeldingHendelseFraKafkaDTO(status) } `should throw`
            IllegalArgumentException::class
    }

    @Test
    fun `burde konvertere til UtdatertFormatBrukerSvar dersom brukerSvar ikke er definert`() {
        val status =
            lagSykmeldingStatusKafkaDTO(
                statusEvent = "SENDT",
                brukerSvarKafkaDTO = null,
                sporsmals =
                    listOf(
                        SporsmalKafkaDTO(
                            shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                            tekst = "",
                            svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                            svar = "ARBEIDSTAKER",
                        ),
                    ),
            )

        val hendelse = sykmeldingHendelseFraKafkaKonverterer.konverterSykmeldingHendelseFraKafkaDTO(status)
        hendelse.brukerSvar.shouldBeInstanceOf<UtdatertFormatBrukerSvar>()
    }

    @Test
    fun `burde konvertere sporsmal liste istedetfor brukerSvar dersom brukerSvar ikke er definert`() {
        val status =
            lagSykmeldingStatusKafkaDTO(
                statusEvent = "SENDT",
                brukerSvarKafkaDTO = null,
                sporsmals =
                    listOf(
                        SporsmalKafkaDTO(
                            shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                            tekst = "",
                            svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                            svar = "ARBEIDSTAKER",
                        ),
                    ),
                arbeidsgiver =
                    ArbeidsgiverStatusKafkaDTO(
                        orgnummer = "org-nr",
                        juridiskOrgnummer = "",
                        orgNavn = "",
                    ),
            )
        val hendelse = sykmeldingHendelseFraKafkaKonverterer.konverterSykmeldingHendelseFraKafkaDTO(status)
        hendelse.brukerSvar.shouldNotBeNull().run {
            arbeidssituasjon.svar `should be equal to` Arbeidssituasjon.ARBEIDSTAKER
        }
    }

    @TestFactory
    fun `burde konvertere status til hendelse status - erAvvist = false`() =
        listOf(
            "APEN" to HendelseStatus.APEN,
            "SENDT" to HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
            "BEKREFTET" to HendelseStatus.SENDT_TIL_NAV,
            "AVBRUTT" to HendelseStatus.AVBRUTT,
            "UTGATT" to HendelseStatus.UTGATT,
        ).map { (originalStatus, forventetStatusEvent) ->
            DynamicTest.dynamicTest("$originalStatus -> $forventetStatusEvent") {
                sykmeldingHendelseFraKafkaKonverterer.konverterStatusTilHendelseStatus(originalStatus, false) `should be equal to`
                    forventetStatusEvent
            }
        }

    @TestFactory
    fun `burde konvertere status til hendelse status - erAvvist = true`() =
        listOf(
            "BEKREFTET" to HendelseStatus.BEKREFTET_AVVIST,
        ).map { (originalStatus, forventetStatusEvent) ->
            DynamicTest.dynamicTest("$originalStatus -> $forventetStatusEvent") {
                sykmeldingHendelseFraKafkaKonverterer.konverterStatusTilHendelseStatus(originalStatus, true) `should be equal to`
                    forventetStatusEvent
            }
        }

    @Nested
    inner class KonverterTilleggsinfo {
        @Test
        fun `arbeidstaker burde konverteres riktig med arbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.ARBEIDSTAKER,
                    arbeidsgiver =
                        ArbeidsgiverStatusKafkaDTO(
                            orgnummer = "orgnummer",
                            juridiskOrgnummer = "juridiskOrgnummer",
                            orgNavn = "orgNavn",
                        ),
                )

            tilleggsinfo.shouldBeInstanceOf<ArbeidstakerTilleggsinfo>().run {
                arbeidsgiver.orgnummer shouldBeEqualTo "orgnummer"
                arbeidsgiver.juridiskOrgnummer shouldBeEqualTo "juridiskOrgnummer"
                arbeidsgiver.orgnavn shouldBeEqualTo "orgNavn"
            }
        }

        @Test
        fun `arbeidstaker burde feile uten arbeidsgiver`() {
            invoking {
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.ARBEIDSTAKER,
                    arbeidsgiver = null,
                )
            }.shouldThrow(Exception::class)
        }

        @Test
        fun `arbeidsledig burde konverteres riktig med tidligereArbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.ARBEIDSLEDIG,
                    tidligereArbeidsgiver =
                        TidligereArbeidsgiverKafkaDTO(
                            orgnummer = "orgnummer",
                            orgNavn = "orgNavn",
                            sykmeldingsId = "_",
                        ),
                )

            tilleggsinfo.shouldBeInstanceOf<ArbeidsledigTilleggsinfo>().run {
                tidligereArbeidsgiver.shouldNotBeNull().run {
                    orgnummer shouldBeEqualTo "orgnummer"
                    orgNavn shouldBeEqualTo "orgNavn"
                }
            }
        }

        @Test
        fun `arbeidsledig burde konverteres riktig uten tidligereArbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.ARBEIDSLEDIG,
                    tidligereArbeidsgiver = null,
                )
            tilleggsinfo.shouldBeInstanceOf<ArbeidsledigTilleggsinfo>().run {
                tidligereArbeidsgiver.shouldBeNull()
            }
        }

        @Test
        fun `permittert burde konverteres riktig med tidligereArbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.PERMITTERT,
                    tidligereArbeidsgiver =
                        TidligereArbeidsgiverKafkaDTO(
                            orgnummer = "orgnummer",
                            orgNavn = "orgNavn",
                            sykmeldingsId = "_",
                        ),
                )

            tilleggsinfo.shouldBeInstanceOf<PermittertTilleggsinfo>().run {
                tidligereArbeidsgiver.shouldNotBeNull().run {
                    orgnummer shouldBeEqualTo "orgnummer"
                    orgNavn shouldBeEqualTo "orgNavn"
                }
            }
        }

        @Test
        fun `permittert burde konverteres riktig uten tidligereArbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.PERMITTERT,
                    tidligereArbeidsgiver = null,
                )
            tilleggsinfo.shouldBeInstanceOf<PermittertTilleggsinfo>().run {
                tidligereArbeidsgiver.shouldBeNull()
            }
        }

        @Test
        fun `fisker burde konverteres riktig med arbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.FISKER,
                    arbeidsgiver =
                        ArbeidsgiverStatusKafkaDTO(
                            orgnummer = "orgnummer",
                            juridiskOrgnummer = "juridiskOrgnummer",
                            orgNavn = "orgNavn",
                        ),
                )

            tilleggsinfo.shouldBeInstanceOf<FiskerTilleggsinfo>().run {
                arbeidsgiver.shouldNotBeNull().run {
                    orgnummer shouldBeEqualTo "orgnummer"
                    juridiskOrgnummer shouldBeEqualTo "juridiskOrgnummer"
                    orgnavn shouldBeEqualTo "orgNavn"
                }
            }
        }

        @Test
        fun `fisker burde konverteres riktig uten arbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.FISKER,
                    arbeidsgiver = null,
                )

            tilleggsinfo.shouldBeInstanceOf<FiskerTilleggsinfo>().run {
                arbeidsgiver.shouldBeNull()
            }
        }

        @Test
        fun `frilanser burde konverteres riktig`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.FRILANSER,
                )

            tilleggsinfo.shouldBeInstanceOf<FrilanserTilleggsinfo>()
        }

        @Test
        fun `naringsdrivende burde konverteres riktig`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.NAERINGSDRIVENDE,
                )

            tilleggsinfo.shouldBeInstanceOf<NaringsdrivendeTilleggsinfo>()
        }

        @Test
        fun `jordbruker burde konverteres riktig`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.JORDBRUKER,
                )

            tilleggsinfo.shouldBeInstanceOf<JordbrukerTilleggsinfo>()
        }

        @Test
        fun `annet arbeidssituasjon burde konverteres riktig`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.ANNET,
                )

            tilleggsinfo.shouldBeInstanceOf<AnnetArbeidssituasjonTilleggsinfo>()
        }

        @Test
        fun `utdatert format burde konverteres riktig med arbeidsgiver og tidlgiereArbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.UTDATERT_FORMAT,
                    arbeidsgiver =
                        ArbeidsgiverStatusKafkaDTO(
                            orgnummer = "orgnummer",
                            juridiskOrgnummer = "juridiskOrgnummer",
                            orgNavn = "orgNavn",
                        ),
                    tidligereArbeidsgiver =
                        TidligereArbeidsgiverKafkaDTO(
                            orgnummer = "tidligereOrgnumemr",
                            orgNavn = "tidligereOrgNavn",
                            sykmeldingsId = "id",
                        ),
                )

            tilleggsinfo.shouldBeInstanceOf<UtdatertFormatTilleggsinfo>().run {
                arbeidsgiver.shouldNotBeNull().run {
                    orgnummer shouldBeEqualTo "orgnummer"
                    juridiskOrgnummer shouldBeEqualTo "juridiskOrgnummer"
                    orgnavn shouldBeEqualTo "orgNavn"
                }
                tidligereArbeidsgiver.shouldNotBeNull().run {
                    orgnummer shouldBeEqualTo "tidligereOrgnumemr"
                    orgNavn shouldBeEqualTo "tidligereOrgNavn"
                }
            }
        }

        @Test
        fun `utdatert format burde konverteres riktig uten noe`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    brukerSvarType = BrukerSvarType.UTDATERT_FORMAT,
                    arbeidsgiver = null,
                    tidligereArbeidsgiver = null,
                )

            tilleggsinfo.shouldBeInstanceOf<UtdatertFormatTilleggsinfo>().run {
                arbeidsgiver.shouldBeNull()
                tidligereArbeidsgiver.shouldBeNull()
            }
        }
    }
}
