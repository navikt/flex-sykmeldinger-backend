package no.nav.helse.flex.config.kafka

class KafkaErrorHandlerException(
    cause: Throwable? = null,
    insecureMessage: String? = null,
) : RuntimeException(
        insecureMessage,
        cause,
    )
