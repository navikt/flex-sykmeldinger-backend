package no.nav.helse.flex

import no.nav.helse.flex.sykmelding.SykmeldingDbRepository
import no.nav.helse.flex.sykmelding.SykmeldingHendelseDbRepository
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykmeldingDbRepositoryFake
import no.nav.helse.flex.testconfig.fakes.SykmeldingHendelseDbRepositoryFake
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FakeRepositoryTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingDbRepository: SykmeldingDbRepository

    @Autowired
    lateinit var sykmeldingHendelseDbRepository: SykmeldingHendelseDbRepository

    @Test
    fun `burde injecte fake sykmeldingDbRepository`() {
        sykmeldingDbRepository.shouldBeInstanceOf(SykmeldingDbRepositoryFake::class)
    }

    @Test
    fun `burde injecte fake sykmeldingHendelseDbRepository`() {
        sykmeldingHendelseDbRepository.shouldBeInstanceOf(SykmeldingHendelseDbRepositoryFake::class)
    }
}
