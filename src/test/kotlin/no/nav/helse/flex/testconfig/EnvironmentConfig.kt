package no.nav.helse.flex.testconfig

import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean

@TestConfiguration
class EnvironmentConfig {
    @Bean
    fun environmentToggles(): EnvironmentTogglesFake = EnvironmentTogglesFake()
}
