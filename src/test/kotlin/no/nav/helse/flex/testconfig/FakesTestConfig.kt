package no.nav.helse.flex.testconfig

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepositoryFake
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.narmesteleder.NarmesteLederRepositoryFake
import no.nav.helse.flex.sykmelding.SykmeldingDbRepository
import no.nav.helse.flex.sykmelding.SykmeldingHendelseDbRepository
import no.nav.helse.flex.testconfig.fakes.*
import no.nav.helse.flex.testconfig.fakes.SykmeldingDbRepositoryFake
import no.nav.helse.flex.testconfig.fakes.SykmeldingHendelseDbRepositoryFake
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.CacheManager
import org.springframework.cache.support.NoOpCacheManager
import org.springframework.context.annotation.Bean

@TestConfiguration
class FakesTestConfig {
    @Bean
    fun nowFactory(): NowFactoryFake = NowFactoryFake()

    @Bean
    fun environmentToggles(): EnvironmentTogglesFake = EnvironmentTogglesFake()

    @Bean
    fun sykmeldingDbRepository(): SykmeldingDbRepository = SykmeldingDbRepositoryFake()

    @Bean
    fun sykmeldingHendelseDbRepository(): SykmeldingHendelseDbRepository = SykmeldingHendelseDbRepositoryFake()

    @Bean
    fun arbeidsforholdRepository(): ArbeidsforholdRepository = ArbeidsforholdRepositoryFake()

    @Bean
    fun narmesteLederRepository(): NarmesteLederRepository = NarmesteLederRepositoryFake()

    @Bean
    fun advisoryLock(): AdvisoryLockFake = AdvisoryLockFake()

    @Bean
    fun pdlClient(): PdlClientFake = PdlClientFake()

    @Bean
    fun aaregClient(): AaregClientFake = AaregClientFake()

    @Bean
    fun eregClient(): EregClientFake = EregClientFake()

    @Bean
    fun sykmeldingStatusKafkaProducer(): SykmeldingStatusKafkaProducerFake = SykmeldingStatusKafkaProducerFake()

    @Bean
    fun cacheManager(): CacheManager = NoOpCacheManager()

    @Bean
    fun syketilfelleClient(): SyketilfelleClientFake = SyketilfelleClientFake()

    @Bean
    fun sykmeldingBrukernotifikasjonProducer(): SykmeldingBrukernotifikasjonProducerFake = SykmeldingBrukernotifikasjonProducerFake()
}
