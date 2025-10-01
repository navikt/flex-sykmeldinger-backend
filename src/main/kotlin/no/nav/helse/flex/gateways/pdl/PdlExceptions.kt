package no.nav.helse.flex.gateways.pdl

open class FunctionalPdlError(
    message: String,
) : RuntimeException(message)

class PdlManglerNavnError(
    message: String,
) : FunctionalPdlError(message)

class PdlManglerFoedselsdatoError(
    message: String,
) : FunctionalPdlError(message)
