package no.nav.helse.flex.config.kafka

class KafkaErrorHandlerException(
    message: String? = null,
    cause: Throwable? = null,
    val errorHandlerLoggingEnabled: Boolean = true,
) : RuntimeException(
        message,
        cause,
    )
