package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.FakesTestOppsett
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import no.nav.helse.flex.sykmelding.logikk.SykmeldingLagrer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingLagrerFakeTest : FakesTestOppsett() {
    @Autowired
    private lateinit var sykmeldingLagrer: SykmeldingLagrer

    @AfterEach
    fun tearDown() {
        slettDatabase()
    }

    @Test
    fun `burde lagre sykmelding`() {
        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            ),
        )

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
    }

    @Test
    fun `burde deduplisere sykmeldinger`() {
        repeat(2) {
            sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(
                SykmeldingMedBehandlingsutfallMelding(
                    sykmelding = lagSykmeldingGrunnlag(id = "1"),
                    validation = lagValidation(),
                ),
            )
        }

        sykmeldingRepository.findAll().size `should be equal to` 1
    }

    @Test
    fun `burde sette status til ny`() {
        val sykmeldingMedBehandlingsutfall =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )
        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(sykmeldingMedBehandlingsutfall)

        val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
        sykmelding.shouldNotBeNull()
        sykmelding.statuser.size `should be equal to` 1
        val status = sykmelding.statuser[0]
        status.status `should be equal to` "APEN"
    }
}
