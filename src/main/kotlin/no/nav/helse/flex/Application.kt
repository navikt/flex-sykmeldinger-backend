package no.nav.helse.flex

import no.nav.helse.flex.utils.LogMarker
import no.nav.helse.flex.utils.logger
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.stereotype.Component

@SpringBootApplication
@EnableCaching
@EnableJwtTokenValidation
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

// For debugging purposes
@Component
class AfterInit : ApplicationRunner {
    val logger = logger()

    override fun run(args: ApplicationArguments) {
        logger.info("Logger til secure logs")
        logger.info(LogMarker.SECURE_LOGS, "Secure Log melding")
    }
}
