package no.nav.helse.flex.sykmelding

open class SykmeldingException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

open class SykmeldingIkkeFunnetException(
    message: String,
    cause: Throwable? = null,
) : SykmeldingException(message, cause)

open class SykmeldingErIkkeDinException(
    message: String,
    cause: Throwable? = null,
) : SykmeldingException(message, cause)

open class SykmeldingHendelseException(
    message: String,
    cause: Throwable? = null,
) : SykmeldingException(message, cause)

open class UgyldigSykmeldingStatusException(
    message: String,
    cause: Throwable? = null,
) : SykmeldingException(message, cause)
