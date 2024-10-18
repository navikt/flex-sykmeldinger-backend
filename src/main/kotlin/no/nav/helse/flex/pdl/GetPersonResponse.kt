package no.nav.helse.flex.pdl

import org.apache.commons.text.WordUtils

data class ResponseError(
    val message: String?,
    val locations: List<ErrorLocation>?,
    val path: List<String>?,
    val extensions: ErrorExtension?,
)

data class HentNavnResponse(
    val data: HentNavnResponseData,
    val errors: List<ResponseError>?,
)

data class HentNavnResponseData(
    val hentPerson: HentNavn? = null,
)

data class HentNavn(
    val navn: List<Navn>? = null,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

fun Navn.format(): String {
    val navn =
        if (mellomnavn != null) {
            "$fornavn $mellomnavn $etternavn"
        } else {
            "$fornavn $etternavn"
        }

    return WordUtils.capitalizeFully(navn, ' ', '-')
}

data class ErrorLocation(
    val line: String?,
    val column: String?,
)

data class ErrorExtension(
    val code: String?,
    val classification: String?,
)
