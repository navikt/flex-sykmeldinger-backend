package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import no.nav.helse.flex.gateways.SykmeldingNotifikasjonStatus
import no.nav.helse.flex.gateways.ereg.Navn
import no.nav.helse.flex.gateways.ereg.Nokkelinfo
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.*
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingStatusBuffer
import org.amshove.kluent.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime

class EksternSykmeldingHandtererTest : FakesTestOppsett() {
    @Autowired
    lateinit var nowFactoryFake: NowFactoryFake

    @Autowired
    private lateinit var eksternSykmeldingHandterer: EksternSykmeldingHandterer

    @Autowired
    private lateinit var eregClient: EregClientFake

    @Autowired
    private lateinit var aaregClient: AaregClientFake

    @Autowired
    private lateinit var sykmeldignHendelseBuffer: SykmeldingStatusBuffer

    @Autowired
    private lateinit var sykmeldingBrukernotifikasjonProducer: SykmeldingBrukernotifikasjonProducerFake

    @Autowired
    private lateinit var sykmeldingStatusKafkaProducer: SykmeldingStatusKafkaProducerFake

    @AfterEach
    fun tearDown() {
        slettDatabase()
        aaregClient.reset()
        nowFactoryFake.reset()
        eregClient.reset()
        sykmeldingBrukernotifikasjonProducer.reset()
        sykmeldingStatusKafkaProducer.reset()
    }

    @Test
    fun `burde lagre sykmelding`() {
        nowFactoryFake.setNow(Instant.parse("2024-01-01T00:00:00Z"))

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "_",
            lagEksternSykmeldingMelding(sykmelding = lagSykmeldingGrunnlag(id = "1")),
        )

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            opprettet shouldBeEqualTo Instant.parse("2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `burde oppdatere sykmelding grunnlag`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr-1")),
                sykmeldingGrunnlagOppdatert = Instant.parse("2020-01-01T00:00:00Z"),
            ),
        )
        nowFactoryFake.setNow(Instant.parse("2024-01-01T00:00:00Z"))

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "_",
            eksternSykmeldingMelding =
                lagEksternSykmeldingMelding(
                    sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient("fnr-2")),
                ),
        )

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            pasientFnr shouldBeEqualTo "fnr-2"
            sykmeldingGrunnlagOppdatert `should be equal to` Instant.parse("2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `burde ikke oppdatere dersom ny kafka melding er lik`() {
        val sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")
        val validation = lagValidation()
        val originaltOpprettet = Instant.parse("2020-01-01T00:00:00Z")
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = sykmeldingGrunnlag,
                validation = validation,
                sykmeldingGrunnlagOppdatert = originaltOpprettet,
                validationOppdatert = originaltOpprettet,
            ),
        )
        nowFactoryFake.setNow(Instant.parse("2024-01-01T00:00:00Z"))

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "_",
            eksternSykmeldingMelding =
                lagEksternSykmeldingMelding(
                    sykmelding = sykmeldingGrunnlag,
                    validation = validation,
                ),
        )
        sykmeldingRepository
            .findBySykmeldingId("1")
            .shouldNotBeNull()
            .run {
                sykmeldingGrunnlagOppdatert `should be equal to` originaltOpprettet
                validationOppdatert `should be equal to` originaltOpprettet
            }
    }

    @Test
    fun `burde oppdatere validation`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(status = RuleType.PENDING),
                validationOppdatert = Instant.parse("2020-01-01T00:00:00Z"),
            ),
        )
        nowFactoryFake.setNow(Instant.parse("2024-01-01T00:00:00Z"))

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "1",
            eksternSykmeldingMelding =
                lagEksternSykmeldingMelding(
                    sykmelding = lagSykmeldingGrunnlag(id = "1"),
                    validation = lagValidation(status = RuleType.OK),
                ),
        )
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            validation.status `should be equal to` RuleType.OK
            validationOppdatert `should be equal to` Instant.parse("2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `burde legge til hendelse med status APEN`() {
        nowFactoryFake.setNow(Instant.parse("2024-01-01T00:00:00Z"))

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "_",
            eksternSykmeldingMelding =
                lagEksternSykmeldingMelding(
                    sykmelding = lagSykmeldingGrunnlag(id = "1"),
                ),
        )

        sykmeldingRepository
            .findBySykmeldingId("1")
            .shouldNotBeNull()
            .hendelser
            .shouldHaveSingleItem()
            .run {
                status shouldBeEqualTo HendelseStatus.APEN
                lokaltOpprettet shouldBeEqualTo Instant.parse("2024-01-01T00:00:00Z")
            }
    }

    @Test
    fun `burde hente arbeidsforhold nar sykmelding lagres`() {
        aaregClient.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(lagArbeidsforholdOversikt(arbeidstakerIdenter = listOf("fnr"), arbeidsstedOrgnummer = "910825518")),
            ),
            "fnr",
        )
        eregClient.setNokkelinfo(failure = RuntimeException())
        eregClient.setNokkelinfo(nokkelinfo = Nokkelinfo(Navn("Org Navn")), orgnummer = "910825518")

        val sykmeldingMedBehandlingsutfall =
            EksternSykmeldingMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr")),
                validation = lagValidation(),
            )

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(sykmeldingId = "_", sykmeldingMedBehandlingsutfall)

        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(listOf("fnr"))
        arbeidsforhold.size `should be equal to` 1
        arbeidsforhold.first().orgnavn `should be equal to` "Org Navn"
    }

    @Test
    fun `burde publisere brukernotifikasjon ved lagring av sykmelding`() {
        val sykmeldingMedBehandlingsutfall = lagEksternSykmeldingMelding()

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(sykmeldingId = "_", sykmeldingMedBehandlingsutfall)

        sykmeldingBrukernotifikasjonProducer.hentSykmeldingBrukernotifikasjoner().shouldHaveSingleItem()
    }

    @Test
    fun `burde lagre alle buffrede hendelser på sykmelding`() {
        sykmeldignHendelseBuffer.leggTil(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event = lagSykmeldingStatusKafkaDTO(statusEvent = "SENDT"),
            ),
        )

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "_",
            lagEksternSykmeldingMelding(sykmelding = lagSykmeldingGrunnlag(id = "1")),
        )

        val sykmelding = sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
        sykmelding.hendelser.size `should be equal to` 2
        sykmelding.hendelser[0].status `should be equal to` HendelseStatus.APEN
        sykmelding.hendelser[1].status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
    }

    @Test
    fun `burde slette sykmelding dersom hendelse er null`() {
        sykmeldingRepository.save(
            lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")),
        )
        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(sykmeldingId = "1", eksternSykmeldingMelding = null)
        sykmeldingRepository.findAll().shouldHaveSize(0)
    }

    @Test
    fun `burde produsere status APEN ved opprettelse av sykmelding`() {
        nowFactoryFake.setNow(Instant.parse("2025-10-24T10:30:01+02:00"))

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "_",
            lagEksternSykmeldingMelding(sykmelding = lagSykmeldingGrunnlag(id = "1")),
        )

        sykmeldingStatusKafkaProducer.sendteSykmeldingStatuser().shouldHaveSingleItem().run {
            event.statusEvent shouldBeEqualTo "APEN"
        }
    }

    @Test
    fun `burde ikke produsere status APEN ved opprettelse av sykmelding før 2025-10-24 klokka 1030`() {
        nowFactoryFake.setNow(Instant.parse("2025-10-24T10:30:00+02:00"))

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "_",
            lagEksternSykmeldingMelding(sykmelding = lagSykmeldingGrunnlag(id = "1")),
        )

        sykmeldingStatusKafkaProducer.sendteSykmeldingStatuser().`should be empty`()
    }

    @Nested
    inner class Companion {
        @Test
        fun `lagNySykmelding burde lage ny sykmelding med APEN hendelse`() {
            val tidspunkt = Instant.parse("2024-01-01T00:00:00Z")
            val eksternSykmeldingMelding =
                lagEksternSykmeldingMelding()

            val nySykmelding =
                EksternSykmeldingHandterer.lagNySykmelding(
                    eksternSykmeldingMelding = eksternSykmeldingMelding,
                    tidspunkt = tidspunkt,
                )

            nySykmelding.run {
                sykmeldingGrunnlag `should be equal to` eksternSykmeldingMelding.sykmelding
                validation `should be equal to` eksternSykmeldingMelding.validation
                sykmeldingGrunnlagOppdatert `should be equal to` tidspunkt
                hendelseOppdatert `should be equal to` tidspunkt
                sykmeldingGrunnlagOppdatert `should be equal to` tidspunkt
                validationOppdatert `should be equal to` tidspunkt
            }
            nySykmelding.hendelser.shouldHaveSingleItem().run {
                status `should be equal to` HendelseStatus.APEN
                hendelseOpprettet `should be equal to`
                    eksternSykmeldingMelding.sykmelding.metadata.mottattDato
                        .toInstant()
                lokaltOpprettet `should be equal to` tidspunkt
                source `should be equal to` SykmeldingHendelse.LOKAL_SOURCE
            }
        }

        @Test
        fun `oppdaterSykmelding burde oppdatere riktig`() {
            val eksisterendeSykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                    validation = lagValidation(status = RuleType.OK),
                )
            val eksternSykmeldingMelding =
                lagEksternSykmeldingMelding(
                    sykmelding = lagSykmeldingGrunnlag(id = "2"),
                    validation = lagValidation(status = RuleType.INVALID),
                )
            val tidspunkt = Instant.parse("2024-01-01T00:00:00Z")
            val oppdatertSykmelding =
                EksternSykmeldingHandterer.oppdaterSykmelding(
                    eksisterendeSykmelding = eksisterendeSykmelding,
                    eksternSykmeldingMelding = eksternSykmeldingMelding,
                    tidspunkt = tidspunkt,
                )
            oppdatertSykmelding.run {
                sykmeldingId `should be equal to` eksternSykmeldingMelding.sykmelding.id
                sykmeldingGrunnlag `should be equal to` eksternSykmeldingMelding.sykmelding
                validation `should be equal to` eksternSykmeldingMelding.validation
                sykmeldingGrunnlagOppdatert `should be equal to` tidspunkt
                validationOppdatert `should be equal to` tidspunkt

                databaseId `should be equal to` eksisterendeSykmelding.databaseId
                hendelser shouldBeEqualTo eksisterendeSykmelding.hendelser
                hendelseOppdatert `should be equal to` eksisterendeSykmelding.hendelseOppdatert
            }
        }

        @TestFactory
        fun `lagSykemldingNotifikasjon burde lage rikitg notifikasjon`() =
            listOf(
                Pair(
                    lagSykmelding(
                        validation = lagValidation(status = RuleType.OK),
                        sykmeldingGrunnlag =
                            lagSykmeldingGrunnlag(
                                id = "1",
                                metadata =
                                    lagSykmeldingMetadata(mottattDato = OffsetDateTime.parse("2020-01-01T00:00:00Z")),
                                pasient = lagPasient(fnr = "fnr"),
                            ),
                    ),
                    SykmeldingNotifikasjon(
                        sykmeldingId = "1",
                        fnr = "fnr",
                        status = SykmeldingNotifikasjonStatus.OK,
                        mottattDato = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
                Pair(
                    lagSykmelding(
                        validation = lagValidation(status = RuleType.INVALID),
                        sykmeldingGrunnlag =
                            lagSykmeldingGrunnlag(
                                id = "2",
                                metadata =
                                    lagSykmeldingMetadata(mottattDato = OffsetDateTime.parse("2022-01-01T00:00:00Z")),
                                pasient = lagPasient(fnr = "fnr-2"),
                            ),
                    ),
                    SykmeldingNotifikasjon(
                        sykmeldingId = "2",
                        fnr = "fnr-2",
                        status = SykmeldingNotifikasjonStatus.INVALID,
                        mottattDato = LocalDateTime.parse("2022-01-01T00:00:00.000"),
                    ),
                ),
            ).mapIndexed { index, (sykmelding, forventetNotifikasjon) ->
                DynamicTest.dynamicTest("Punkt $index") {
                    val notifikasjon = EksternSykmeldingHandterer.lagSykemldingNotifikasjon(sykmelding)
                    notifikasjon shouldBeEqualTo forventetNotifikasjon
                }
            }
    }
}
