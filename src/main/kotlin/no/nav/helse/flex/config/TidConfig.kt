package no.nav.helse.flex.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.util.function.Supplier

@Configuration
class TidConfig {
    @Bean
    fun nowFactory(): Supplier<Instant> {
        return Supplier { Instant.now() }
    }
}
