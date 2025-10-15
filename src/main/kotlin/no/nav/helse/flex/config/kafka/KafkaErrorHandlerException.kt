package no.nav.helse.flex.config.kafka

class KafkaErrorHandlerException(
    cause: Throwable? = null,
    insensitiveMessage: String? = null,
) : RuntimeException(
        insensitiveMessage,
        cause,
    )
