package no.nav.helse.flex.testconfig

import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testconfig.fakes.NowFactoryFake
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class IntegrasjonsTestConfig {
    @Bean
    fun environmentToggles(): EnvironmentTogglesFake = EnvironmentTogglesFake()

    @Bean
    fun nowFactory(): NowFactoryFake = NowFactoryFake()
}
