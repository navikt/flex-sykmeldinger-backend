package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.ereg.Navn
import no.nav.helse.flex.clients.ereg.Nokkelinfo
import no.nav.helse.flex.sykmelding.application.SykmeldingKafkaLagrer
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.MetadataType
import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EregClientFake
import no.nav.helse.flex.testconfig.fakes.NowFactoryFake
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
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
    }

    @Test
    fun `burde lagre sykmelding`() {
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(
            lagSykmeldingKafkaRecord(sykmelding = lagSykmeldingGrunnlag(id = "1")),
        )

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
    }

    @Test
    fun `burde oppdatere sykmeldinger`() {
        nowFactoryFake.setNow(Instant.parse("2024-01-01T00:00:00Z"))
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(
            lagSykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient("fnr")),
                validation = lagValidation(status = RuleType.PENDING),
                metadata = lagMeldingsinformasjonEnkel(),
            ),
        )

        nowFactoryFake.setNow(Instant.parse("2025-01-01T00:00:00Z"))
        sykmeldingKafkaLagrer.lagreSykmeldingMedBehandlingsutfall(
            lagSykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient("ny_fnr")),
                validation = lagValidation(status = RuleType.OK),
                metadata = lagMeldingsinformasjonEgenmeldt(),
            ),
        )

        sykmeldingRepository.findAll().size `should be equal to` 1
        sykmeldingRepository
            .findBySykmeldingId("1")
            .`should not be null`()
            .also {
                it.sykmeldingGrunnlag.pasient.fnr shouldBeEqualTo "ny_fnr"
                it.validation.status `should be equal to` RuleType.OK
                it.meldingsinformasjon.type `should be equal to` MetadataType.EGENMELDT
                it.sykmeldingGrunnlagOppdatert `should be equal to` Instant.parse("2025-01-01T00:00:00Z")
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
