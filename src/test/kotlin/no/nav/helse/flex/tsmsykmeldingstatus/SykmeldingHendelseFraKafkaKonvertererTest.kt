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
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
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

    @ParameterizedTest
    @EnumSource(ArbeidssituasjonDTO::class)
    fun `burde konvertere BrukerSvarKafkaDto til BrukerSvar`(arbeidssituasjonDTO: ArbeidssituasjonDTO) {
        val brukerSvarKafkaDTO = lagBrukerSvarKafkaDto(arbeidssituasjonDTO)

        val konvertert = sykmeldingHendelseFraKafkaKonverterer.konverterBrukerSvarKafkaDtoTilBrukerSvar(brukerSvarKafkaDTO)
        konvertert.uriktigeOpplysninger?.svar `should be equal to` listOf(UriktigeOpplysning.PERIODE)
        konvertert.erOpplysningeneRiktige.svar `should be equal to` true
        konvertert.arbeidssituasjonSporsmal.svar.name `should be equal to` arbeidssituasjonDTO.name

        when (arbeidssituasjonDTO) {
            ArbeidssituasjonDTO.ARBEIDSTAKER -> {
                val arbeidstakerBrukerSvar = konvertert as? ArbeidstakerBrukerSvar
                arbeidstakerBrukerSvar.`should not be null`()
                arbeidstakerBrukerSvar.arbeidsgiverOrgnummer.svar `should be equal to` "123456789"
                arbeidstakerBrukerSvar.riktigNarmesteLeder?.svar `should be equal to` true
                arbeidstakerBrukerSvar.harEgenmeldingsdager?.svar `should be equal to` true
                arbeidstakerBrukerSvar.egenmeldingsdager?.svar `should be equal to` listOf(LocalDate.parse("2021-01-01"))
            }
            ArbeidssituasjonDTO.FRILANSER -> {
                val frilanserBrukerSvar = konvertert as? FrilanserBrukerSvar
                frilanserBrukerSvar.`should not be null`()
                frilanserBrukerSvar.harBruktEgenmelding?.svar `should be equal to` true
                frilanserBrukerSvar.egenmeldingsperioder?.svar `should be equal to`
                    listOf(
                        Egenmeldingsperiode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05")),
                        Egenmeldingsperiode(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15")),
                    )
                frilanserBrukerSvar.harForsikring?.svar `should be equal to` true
            }
            ArbeidssituasjonDTO.NAERINGSDRIVENDE -> {
                val naeringsdrivendeBrukerSvar = konvertert as? NaringsdrivendeBrukerSvar
                naeringsdrivendeBrukerSvar.`should not be null`()
                naeringsdrivendeBrukerSvar.harBruktEgenmelding?.svar `should be equal to` true
                naeringsdrivendeBrukerSvar.egenmeldingsperioder?.svar `should be equal to`
                    listOf(
                        Egenmeldingsperiode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05")),
                        Egenmeldingsperiode(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15")),
                    )
                naeringsdrivendeBrukerSvar.harForsikring?.svar `should be equal to` true
            }
            ArbeidssituasjonDTO.FISKER -> {
                val fiskerBrukerSvar = konvertert as? FiskerBrukerSvar
                fiskerBrukerSvar.`should not be null`()
                fiskerBrukerSvar.lottOgHyre.svar `should be equal to` FiskerLottOgHyre.LOTT
                fiskerBrukerSvar.blad.svar `should be equal to` FiskerBlad.A
            }
            ArbeidssituasjonDTO.JORDBRUKER -> {
                val jordbrukerBrukerSvar = konvertert as? JordbrukerBrukerSvar
                jordbrukerBrukerSvar.`should not be null`()
                jordbrukerBrukerSvar.harBruktEgenmelding?.svar `should be equal to` true
                jordbrukerBrukerSvar.egenmeldingsperioder?.svar `should be equal to`
                    listOf(
                        Egenmeldingsperiode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-05")),
                        Egenmeldingsperiode(LocalDate.parse("2025-01-10"), LocalDate.parse("2025-01-15")),
                    )
                jordbrukerBrukerSvar.harForsikring?.svar `should be equal to` true
            }
            ArbeidssituasjonDTO.ARBEIDSLEDIG -> {
                val arbeidsledigBrukerSvar = konvertert as? ArbeidsledigBrukerSvar
                arbeidsledigBrukerSvar.`should not be null`()
                arbeidsledigBrukerSvar.arbeidsledigFraOrgnummer?.svar `should be equal to` "123456789"
            }
            ArbeidssituasjonDTO.PERMITTERT -> {
                val permittertBrukerSvar = konvertert as? PermittertBrukerSvar
                permittertBrukerSvar.`should not be null`()
                permittertBrukerSvar.arbeidsledigFraOrgnummer?.svar `should be equal to` "123456789"
            }
            ArbeidssituasjonDTO.ANNET -> {
                val annetBrukerSvar = konvertert as? AnnetArbeidssituasjonBrukerSvar
                annetBrukerSvar.`should not be null`()
            }
        }
    }

    @Nested
    inner class KonverterSporsmalsTilBrukerSvar {
        @Test
        fun `burde returnere null dersom ingen spørmål`() {
            sykmeldingHendelseFraKafkaKonverterer.konverterSporsmalTilBrukerSvar(emptyList()).shouldBeNull()
        }

        @Test
        fun `burde konvertere minimalt antall spørsmål til brukerSvar`() {
            val sporsmals =
                listOf(
                    SporsmalKafkaDTO(
                        tekst = "Jeg er sykmeldt som",
                        shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                        svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                        svar = "ARBEIDSTAKER",
                    ),
                )
            val brukerSvar = sykmeldingHendelseFraKafkaKonverterer.konverterSporsmalTilBrukerSvar(sporsmals)
            brukerSvar
                .shouldNotBeNull()
                .shouldBeInstanceOf<ArbeidstakerBrukerSvar>()
                .run {
                    arbeidssituasjon `should be equal to` Arbeidssituasjon.ARBEIDSTAKER
                    arbeidssituasjonSporsmal.run {
                        sporsmaltekst `should be equal to` "Jeg er sykmeldt som"
                        svar `should be equal to` Arbeidssituasjon.ARBEIDSTAKER
                    }
                    arbeidsgiverOrgnummer.run {
                        sporsmaltekst shouldBeEqualTo ""
                        svar shouldBeEqualTo ""
                    }
                }
        }
    }

    @Nested
    inner class KonverterTilleggsinfo {
        @Test
        fun `arbeidstaker burde konverteres riktig med arbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER,
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
                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER,
                    arbeidsgiver = null,
                )
            }.shouldThrow(Exception::class)
        }

        @Test
        fun `arbeidsledig burde konverteres riktig med tidligereArbeidsgiver`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG,
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
                    arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG,
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
                    arbeidssituasjon = Arbeidssituasjon.PERMITTERT,
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
                    arbeidssituasjon = Arbeidssituasjon.PERMITTERT,
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
                    arbeidssituasjon = Arbeidssituasjon.FISKER,
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
                    arbeidssituasjon = Arbeidssituasjon.FISKER,
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
                    arbeidssituasjon = Arbeidssituasjon.FRILANSER,
                )

            tilleggsinfo.shouldBeInstanceOf<FrilanserTilleggsinfo>()
        }

        @Test
        fun `naringsdrivende burde konverteres riktig`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    arbeidssituasjon = Arbeidssituasjon.NAERINGSDRIVENDE,
                )

            tilleggsinfo.shouldBeInstanceOf<NaringsdrivendeTilleggsinfo>()
        }

        @Test
        fun `jordbruker burde konverteres riktig`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    arbeidssituasjon = Arbeidssituasjon.JORDBRUKER,
                )

            tilleggsinfo.shouldBeInstanceOf<JordbrukerTilleggsinfo>()
        }

        @Test
        fun `annet arbeidssituasjon burde konverteres riktig`() {
            val tilleggsinfo =
                sykmeldingHendelseFraKafkaKonverterer.konverterTilTilleggsinfo(
                    arbeidssituasjon = Arbeidssituasjon.ANNET,
                )

            tilleggsinfo.shouldBeInstanceOf<AnnetArbeidssituasjonTilleggsinfo>()
        }
    }
}
