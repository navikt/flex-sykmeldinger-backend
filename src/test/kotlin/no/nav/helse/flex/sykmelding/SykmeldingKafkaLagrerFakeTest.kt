package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.ereg.Navn
import no.nav.helse.flex.clients.ereg.Nokkelinfo
import no.nav.helse.flex.sykmelding.application.SykmeldingKafkaLagrer
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EregClientFake
import no.nav.helse.flex.testconfig.fakes.NowFactoryFake
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class SykmeldingKafkaLagrerFakeTest : FakesTestOppsett() {
    @Autowired
    lateinit var nowFactoryFake: NowFactoryFake

    @Autowired
    private lateinit var sykmeldingKafkaLagrer: SykmeldingKafkaLagrer

    @Autowired
    private lateinit var eregClient: EregClientFake

    @Autowired
    private lateinit var aaregClient: AaregClientFake

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
    fun `burde ikke oppdatere meldingsinformasjon`() {
        val kafkaMelding =
            lagSykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                metadata = lagMeldingsinformasjonEnkel(),
            )
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(kafkaMelding)

        invoking {
            sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(
                kafkaMelding.copy(
                    metadata = lagMeldingsinformasjonEgenmeldt(),
                ),
            )
        }.shouldThrow(RuntimeException::class)
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
    fun `burde sette status til ny`() {
        val sykmeldingKafkaRecord = lagSykmeldingKafkaRecord(sykmelding = lagSykmeldingGrunnlag(id = "1"))
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingKafkaRecord)

        val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
        sykmelding.shouldNotBeNull()
        sykmelding.hendelser.size `should be equal to` 1
        val status = sykmelding.hendelser[0]
        status.status `should be equal to` HendelseStatus.APEN
    }

    @Test
    fun `burde hente arbeidsforhold nar sykmelding lagres`() {
        aaregClient.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(listOf(lagArbeidsforholdOversikt(identer = listOf("fnr"), orgnummer = "910825518"))),
            "fnr",
        )
        eregClient.setNokkelinfo(failure = RuntimeException())
        eregClient.setNokkelinfo(nokkelinfo = Nokkelinfo(Navn("Org Navn")), orgnummer = "910825518")

        val sykmeldingMedBehandlingsutfall =
            SykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr")),
                validation = lagValidation(),
                metadata = lagMeldingsinformasjonEgenmeldt(),
            )

        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)

        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(listOf("fnr"))
        arbeidsforhold.size `should be equal to` 1
        arbeidsforhold.first().orgnavn `should be equal to` "Org Navn"
    }
}
