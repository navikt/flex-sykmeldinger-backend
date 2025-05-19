package no.nav.helse.flex

import no.nav.helse.flex.sykmelding.SykmeldingDbRepositoryFake
import no.nav.helse.flex.sykmelding.SykmeldingHendelseDbRepositoryFake
import no.nav.helse.flex.sykmelding.domain.SykmeldingDbRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelseDbRepository
import no.nav.helse.flex.testconfig.FakesTestOppsett
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
