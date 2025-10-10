package no.nav.helse.flex.config.kafka

class KafkaErrorHandlerException(
    cause: Throwable,
    val skalLogges: Boolean = true,
) : RuntimeException(
        "Se årsak",
        cause,
    )
