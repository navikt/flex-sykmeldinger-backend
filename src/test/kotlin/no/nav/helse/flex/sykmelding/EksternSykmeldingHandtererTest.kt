package no.nav.helse.flex.sykmelding

import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.gateways.SykmeldingNotifikasjon
import no.nav.helse.flex.gateways.SykmeldingNotifikasjonStatus
import no.nav.helse.flex.gateways.ereg.HentOrganisasjonerResponse
import no.nav.helse.flex.gateways.ereg.Navn
import no.nav.helse.flex.gateways.ereg.OrganisasjonInfo
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.*
import no.nav.helse.flex.testdata.*
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import java.time.*

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
            opprettet shouldBe Instant.parse("2024-01-01T00:00:00Z")
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
            pasientFnr shouldBe "fnr-2"
            sykmeldingGrunnlagOppdatert shouldBe Instant.parse("2024-01-01T00:00:00Z")
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
                sykmeldingGrunnlagOppdatert shouldBe originaltOpprettet
                validationOppdatert shouldBe originaltOpprettet
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
            validation.status shouldBe RuleType.OK
            validationOppdatert shouldBe Instant.parse("2024-01-01T00:00:00Z")
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
            .shouldBeSingle()
            .run {
                status shouldBe HendelseStatus.APEN
                lokaltOpprettet shouldBe Instant.parse("2024-01-01T00:00:00Z")
            }
    }

    @Test
    fun `burde hente arbeidsforhold nar sykmelding lagres selv om henting fra ereg feiler en gang`() {
        aaregClient.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(
                listOf(lagArbeidsforholdOversikt(arbeidstakerIdenter = listOf("fnr"), arbeidsstedOrgnummer = "910825518")),
            ),
            "fnr",
        )
        eregClient.setHentOrganisasjonerResponse(
            failure = RuntimeException(),
        )
        eregClient.setHentOrganisasjonerResponse(
            HentOrganisasjonerResponse(
                organisasjoner = mapOf("910825518" to OrganisasjonInfo(Navn("Testbedriften AS"))),
            ),
        )

        val sykmeldingMedBehandlingsutfall =
            EksternSykmeldingMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr")),
                validation = lagValidation(),
            )

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(sykmeldingId = "_", sykmeldingMedBehandlingsutfall)

        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(listOf("fnr"))
        arbeidsforhold.size shouldBe 1
        arbeidsforhold.first().orgnavn shouldBe "Testbedriften AS"
    }

    @Test
    fun `burde publisere brukernotifikasjon ved lagring av sykmelding`() {
        val sykmeldingMedBehandlingsutfall = lagEksternSykmeldingMelding()

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(sykmeldingId = "_", sykmeldingMedBehandlingsutfall)

        sykmeldingBrukernotifikasjonProducer.hentSykmeldingBrukernotifikasjoner().shouldBeSingle()
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
        nowFactoryFake.setNow(
            ZonedDateTime.of(LocalDateTime.parse("2025-10-24T10:35:01"), ZoneId.of("Europe/Oslo")).toInstant(),
        )

        eksternSykmeldingHandterer.lagreSykmeldingFraKafka(
            sykmeldingId = "_",
            lagEksternSykmeldingMelding(sykmelding = lagSykmeldingGrunnlag(id = "1")),
        )

        sykmeldingStatusKafkaProducer.sendteSykmeldingStatuser().shouldBeSingle().run {
            event.statusEvent shouldBe "APEN"
        }
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
                sykmeldingGrunnlag shouldBe eksternSykmeldingMelding.sykmelding
                validation shouldBe eksternSykmeldingMelding.validation
                sykmeldingGrunnlagOppdatert shouldBe tidspunkt
                hendelseOppdatert shouldBe tidspunkt
                sykmeldingGrunnlagOppdatert shouldBe tidspunkt
                validationOppdatert shouldBe tidspunkt
            }
            nySykmelding.hendelser.shouldBeSingle().run {
                status shouldBe HendelseStatus.APEN
                hendelseOpprettet shouldBe
                    eksternSykmeldingMelding.sykmelding.metadata.mottattDato
                        .toInstant()
                lokaltOpprettet shouldBe tidspunkt
                source shouldBe SykmeldingHendelse.LOKAL_SOURCE
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
                sykmeldingId shouldBe eksternSykmeldingMelding.sykmelding.id
                sykmeldingGrunnlag shouldBe eksternSykmeldingMelding.sykmelding
                validation shouldBe eksternSykmeldingMelding.validation
                sykmeldingGrunnlagOppdatert shouldBe tidspunkt
                validationOppdatert shouldBe tidspunkt

                databaseId shouldBe eksisterendeSykmelding.databaseId
                hendelser shouldBe eksisterendeSykmelding.hendelser
                hendelseOppdatert shouldBe eksisterendeSykmelding.hendelseOppdatert
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
                    notifikasjon shouldBe forventetNotifikasjon
                }
            }
    }
}
