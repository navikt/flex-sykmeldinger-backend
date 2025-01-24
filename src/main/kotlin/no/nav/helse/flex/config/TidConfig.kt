package no.nav.helse.flex.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.function.Supplier

@Configuration
class TidConfig {
    @Bean
    fun nowFactory(): Supplier<Instant> = Supplier { Instant.now() }
}

val norskTidssone = ZoneId.of("Europe/Oslo")

fun Supplier<Instant>.getDagensDatoINorge(): LocalDate = LocalDate.ofInstant(this.get(), norskTidssone)
