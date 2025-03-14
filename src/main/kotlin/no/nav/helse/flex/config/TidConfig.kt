package no.nav.helse.flex.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.*
import java.util.*
import java.util.function.Supplier

@Configuration
class TidConfig {
    @Bean
    fun nowFactory(): Supplier<Instant> = Supplier { Instant.now() }
}

val norskTidssone = ZoneId.of("Europe/Oslo")

fun Instant.tilNorgeOffsetDateTime(): OffsetDateTime = this.atZone(norskTidssone).toOffsetDateTime()

fun Instant.tilNorgeLocalDateTime(): LocalDateTime = this.tilNorgeOffsetDateTime().toLocalDateTime()

fun Instant.tilNorgeLocalDate(): LocalDate = this.tilNorgeLocalDateTime().toLocalDate()

fun OffsetDateTime.tilNorgeTidssone(): OffsetDateTime = this.atZoneSameInstant(norskTidssone).toOffsetDateTime()

fun OffsetDateTime.tilNorgeLocalDateTime(): LocalDateTime = this.toInstant().tilNorgeLocalDateTime()

fun LocalDateTime.tilNorgeOffsetDateTime(): OffsetDateTime = this.atZone(norskTidssone).toOffsetDateTime()
