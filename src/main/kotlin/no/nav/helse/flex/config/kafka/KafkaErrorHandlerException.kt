package no.nav.helse.flex.config.kafka

class KafkaErrorHandlerException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(
        message,
        cause,
    )
