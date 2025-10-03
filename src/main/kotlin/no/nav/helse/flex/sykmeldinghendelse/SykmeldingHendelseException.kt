package no.nav.helse.flex.sykmeldinghendelse

open class SykmeldingHendelseException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

open class UgyldigSykmeldingStatusException(
    message: String,
    cause: Throwable? = null,
) : SykmeldingHendelseException(message, cause)

open class KunneIkkeFinneTilleggsinfoException(
    message: String,
    cause: Throwable? = null,
) : SykmeldingHendelseException(message, cause)
