package no.nav.helse.flex

import no.nav.helse.flex.sykmelding.SykmeldingRepositoryFake
import no.nav.helse.flex.testconfig.FakesTestOppsett
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class FakeRepositoryTest : FakesTestOppsett() {
    @Test
    fun `burde injecte fake repository`() {
        sykmeldingRepository.shouldBeInstanceOf(SykmeldingRepositoryFake::class)
    }
}
