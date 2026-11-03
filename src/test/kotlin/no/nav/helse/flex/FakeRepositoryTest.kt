package no.nav.helse.flex

import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helse.flex.sykmelding.SykmeldingDbRepository
import no.nav.helse.flex.sykmelding.SykmeldingHendelseDbRepository
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykmeldingDbRepositoryFake
import no.nav.helse.flex.testconfig.fakes.SykmeldingHendelseDbRepositoryFake
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class FakeRepositoryTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingDbRepository: SykmeldingDbRepository

    @Autowired
    lateinit var sykmeldingHendelseDbRepository: SykmeldingHendelseDbRepository

    @Test
    fun `burde injecte fake sykmeldingDbRepository`() {
        sykmeldingDbRepository.shouldBeInstanceOf<SykmeldingDbRepositoryFake>()
    }

    @Test
    fun `burde injecte fake sykmeldingHendelseDbRepository`() {
        sykmeldingHendelseDbRepository.shouldBeInstanceOf<SykmeldingHendelseDbRepositoryFake>()
    }
}
