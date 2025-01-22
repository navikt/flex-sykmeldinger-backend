package no.nav.helse.flex.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.LocalDate
import java.util.function.Supplier

@Configuration
class TidConfig {
    @Bean
    fun nowFactory(): Supplier<Instant> = Supplier { Instant.now() }
}

fun Supplier<Instant>.getDagensDatoINorge(): LocalDate {
    // TODO: Bruk norsk tidssone
    return LocalDate.ofInstant(this.get(), null)
}
