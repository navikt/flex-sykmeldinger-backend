package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversikt
import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.ereg.Navn
import no.nav.helse.flex.clients.ereg.Nokkelinfo
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.logikk.SykmeldingLagrer
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EregClientFake
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingLagrerFakeTest : FakesTestOppsett() {
    @Autowired
    private lateinit var sykmeldingLagrer: SykmeldingLagrer

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
        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(
            SykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
                metadata = lagMeldingsinformasjonEgenmeldt(),
            ),
        )

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
    }

    @Test
    fun `burde deduplisere sykmeldinger`() {
        repeat(2) {
            sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(
                SykmeldingKafkaRecord(
                    sykmelding = lagSykmeldingGrunnlag(id = "1"),
                    validation = lagValidation(),
                    metadata = lagMeldingsinformasjonEgenmeldt(),
                ),
            )
        }

        sykmeldingRepository.findAll().size `should be equal to` 1
    }

    @Test
    fun `burde sette status til ny`() {
        val sykmeldingMedBehandlingsutfall =
            SykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
                metadata = lagMeldingsinformasjonEgenmeldt(),
            )
        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)

        val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
        sykmelding.shouldNotBeNull()
        sykmelding.statuser.size `should be equal to` 1
        val status = sykmelding.statuser[0]
        status.status `should be equal to` HendelseStatus.APEN
    }

    @Test
    fun `burde hente arbeidsforhold nar sykmelding lagres`() {
        aaregClient.setArbeidsforholdoversikt(
            lagArbeidsforholdOversiktResponse(listOf(lagArbeidsforholdOversikt(fnr = "fnr", orgnummer = "910825518"))),
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

        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)

        val arbeidsforhold = arbeidsforholdRepository.getAllByFnr("fnr")
        arbeidsforhold.size `should be equal to` 1
        arbeidsforhold.first().orgnavn `should be equal to` "Org Navn"
    }
}
