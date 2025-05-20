package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.ereg.Navn
import no.nav.helse.flex.clients.ereg.Nokkelinfo
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EregClientFake
import no.nav.helse.flex.testconfig.fakes.NowFactoryFake
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingStatusBuffer
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit

class SykmeldingKafkaLagrerTest : FakesTestOppsett() {
    @Autowired
    lateinit var nowFactoryFake: NowFactoryFake

    @Autowired
    private lateinit var sykmeldingKafkaLagrer: SykmeldingKafkaLagrer

    @Autowired
    private lateinit var eregClient: EregClientFake

    @Autowired
    private lateinit var aaregClient: AaregClientFake

    @Autowired
    private lateinit var sykmeldignHendelseBuffer: SykmeldingStatusBuffer

    @AfterEach
    fun tearDown() {
        slettDatabase()
        aaregClient.reset()
        nowFactoryFake.reset()
    }

    @Test
    fun `burde lagre sykmelding`() {
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(
            lagSykmeldingKafkaRecord(sykmelding = lagSykmeldingGrunnlag(id = "1")),
        )

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
    }

    @Test
    fun `burde oppdatere sykmelding grunnlag`() {
        nowFactoryFake.setNow(Instant.parse("2024-01-01T00:00:00Z"))
        val kafkaMelding =
            lagSykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient("fnr")),
            )
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(kafkaMelding)

        nowFactoryFake.setNow(Instant.parse("2025-01-01T00:00:00Z"))
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(
            kafkaMelding.copy(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient("ny_fnr")),
            ),
        )

        sykmeldingRepository.findAll().size `should be equal to` 1
        sykmeldingRepository
            .findBySykmeldingId("1")
            .`should not be null`()
            .also {
                it.sykmeldingGrunnlag.pasient.fnr shouldBeEqualTo "ny_fnr"
                it.sykmeldingGrunnlagOppdatert `should be equal to` Instant.parse("2025-01-01T00:00:00Z")
            }
    }

    @Test
    fun `burde ikke oppdatere dersom ny kafka melding er lik`() {
        val kafkaMelding =
            lagSykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
            )

        val førsteMeldingTid = Instant.parse("2024-01-01T00:00:00Z")
        nowFactoryFake.setNow(førsteMeldingTid)
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(kafkaMelding)

        val nyMeldingTid = førsteMeldingTid.plus(1, ChronoUnit.DAYS)
        nowFactoryFake.setNow(nyMeldingTid)
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(kafkaMelding.copy())

        sykmeldingRepository
            .findBySykmeldingId("1")
            .`should not be null`()
            .also {
                it.sykmeldingGrunnlagOppdatert `should be equal to` førsteMeldingTid
                it.validationOppdatert `should be equal to` førsteMeldingTid
            }
    }

    @Test
    fun `burde oppdatere validation`() {
        nowFactoryFake.setNow(Instant.parse("2024-01-01T00:00:00Z"))
        val kafkaMelding =
            lagSykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(status = RuleType.PENDING),
            )
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(kafkaMelding)

        nowFactoryFake.setNow(Instant.parse("2025-01-01T00:00:00Z"))
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(
            kafkaMelding.copy(
                validation = lagValidation(status = RuleType.OK),
            ),
        )

        sykmeldingRepository.findAll().size `should be equal to` 1
        sykmeldingRepository
            .findBySykmeldingId("1")
            .`should not be null`()
            .also {
                it.validation.status `should be equal to` RuleType.OK
                it.validationOppdatert `should be equal to` Instant.parse("2025-01-01T00:00:00Z")
            }
    }

    @Test
    fun `burde legge til hendelse med status APEN`() {
        val now = Instant.parse("2024-01-01T00:00:00Z")
        nowFactoryFake.setNow(now)
        val sykmeldingKafkaRecord = lagSykmeldingKafkaRecord(sykmelding = lagSykmeldingGrunnlag(id = "1"))
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingKafkaRecord)

        val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
        sykmelding
            .shouldNotBeNull()
            .hendelser
            .shouldHaveSize(1)
            .first()
            .run {
                status `should be equal to` HendelseStatus.APEN
                hendelseOpprettet `should be equal to` now
                lokaltOpprettet `should be equal to` now
                source `should be equal to` SykmeldingHendelse.LOKAL_SOURCE
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
            SykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr")),
                validation = lagValidation(),
            )

        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)

        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(listOf("fnr"))
        arbeidsforhold.size `should be equal to` 1
        arbeidsforhold.first().orgnavn `should be equal to` "Org Navn"
    }

    @Test
    fun `burde lagre alle buffrede hendelser på sykmelding`() {
        sykmeldignHendelseBuffer.leggTil(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event = lagSykmeldingStatusKafkaDTO(statusEvent = "SENDT"),
            ),
        )

        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(
            lagSykmeldingKafkaRecord(sykmelding = lagSykmeldingGrunnlag(id = "1")),
        )

        val sykmelding = sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
        sykmelding.hendelser.size `should be equal to` 2
        sykmelding.hendelser[0].status `should be equal to` HendelseStatus.APEN
        sykmelding.hendelser[1].status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
    }
}
