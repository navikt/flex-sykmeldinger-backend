package no.nav.helse.flex.config.kafka

class KafkaErrorHandlerException(
    message: String,
    cause: Throwable,
    val skalLogges: Boolean = true,
) : RuntimeException(
        message,
        cause,
    )
