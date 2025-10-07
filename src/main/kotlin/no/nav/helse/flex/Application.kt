package no.nav.helse.flex

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableCaching
@EnableJwtTokenValidation
class Application

@Profile("default")
@Configuration
@EnableScheduling
@EnableRetry(proxyTargetClass = true)
class DeployApplicationConfig

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
