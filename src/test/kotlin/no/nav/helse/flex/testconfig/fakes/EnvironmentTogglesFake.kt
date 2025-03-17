package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.config.EnvironmentToggles

class EnvironmentTogglesFake : EnvironmentToggles {
    private var environment = "dev"

    override fun isProduction(): Boolean = environment == "prod"

    override fun isDevelopment(): Boolean = environment == "dev"

    fun setEnvironment(environment: String) {
        this.environment = environment
    }
}
