package no.nav.helse.flex.pdl

open class FunctionalPdlError(
    message: String,
) : RuntimeException(message)

class PdlManglerNavnError(
    message: String,
) : FunctionalPdlError(message)
