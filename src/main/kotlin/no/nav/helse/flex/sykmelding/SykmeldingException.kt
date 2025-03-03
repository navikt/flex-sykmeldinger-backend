package no.nav.helse.flex.sykmelding

open class SykmeldingException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class SykmeldingIkkeFunnetException : SykmeldingException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class SykmeldingErIkkeDinException : SykmeldingException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
