package no.nav.helse.flex.testconfig

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepositoryFake
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.narmesteleder.NarmesteLederRepositoryFake
import no.nav.helse.flex.sykmelding.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.SykmeldingRepositoryFake
import no.nav.helse.flex.testconfig.fakes.AaregClientFake
import no.nav.helse.flex.testconfig.fakes.EregClientFake
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class FakesTestConfig {
    @Bean
    fun sykmeldingRepository(): ISykmeldingRepository = SykmeldingRepositoryFake()

    @Bean
    fun arbeidsforholdRepository(): ArbeidsforholdRepository = ArbeidsforholdRepositoryFake()

    @Bean
    fun narmesteLederRepository(): NarmesteLederRepository = NarmesteLederRepositoryFake()

    @Bean
    fun pdlClient(): PdlClientFake = PdlClientFake()

    @Bean
    fun aaregClient(): AaregClientFake = AaregClientFake()

    @Bean
    fun eregClient(): EregClientFake = EregClientFake()
}
