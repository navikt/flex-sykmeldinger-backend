package no.nav.helse.flex

import no.nav.helse.flex.sykmelding.SykmeldingRepositoryFake
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import no.nav.helse.flex.sykmelding.logikk.SykmeldingLagrer
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FakeRepositoryTest : FakesTestOppsett() {
    @Autowired
    private lateinit var sykmeldingLagrer: SykmeldingLagrer

    @Test
    fun `burde injecte fake repository`() {
        sykmeldingRepository.shouldBeInstanceOf(SykmeldingRepositoryFake::class)
    }

    @Test
    fun `sykmelding lagrer bruker fake repo`() {
        val kafkaMelding =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        sykmeldingLagrer.lagreSykmeldingMedBehandlingsutfall(kafkaMelding)

        sykmeldingRepository.shouldBeInstanceOf(SykmeldingRepositoryFake::class)
        sykmeldingRepository.findAll().size shouldBe 1
    }
}
