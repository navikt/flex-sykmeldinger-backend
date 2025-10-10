package no.nav.helse.flex.config.kafka

class KafkaErrorHandlerException(
    cause: Throwable,
    insecureMessage: String? = null,
    val skalLogges: Boolean = true,
) : RuntimeException(
        insecureMessage,
        cause,
    )
