package no.nav.helse.flex.sykmeldinghendelse

open class TilleggsinfoException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

open class KunneIkkeFinneTilleggsinfoException(
    message: String,
    cause: Throwable? = null,
) : TilleggsinfoException(message, cause)
