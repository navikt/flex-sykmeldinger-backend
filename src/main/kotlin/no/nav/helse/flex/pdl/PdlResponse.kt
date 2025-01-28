package no.nav.helse.flex.pdl

import org.apache.commons.text.WordUtils

data class HentIdenterResponseData(
    val hentIdenter: HentIdenter? = null,
)

data class HentIdenter(
    val identer: List<PdlIdent>,
)

data class PdlIdent(
    val gruppe: String,
    val ident: String,
)

data class GetPersonResponseData(
    val hentPerson: HentPerson? = null,
)

data class HentPerson(
    val navn: List<Navn>? = null,
)

data class Navn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
) {
    fun formatert(): String {
        val navn =
            if (mellomnavn != null) {
                "$fornavn $mellomnavn $etternavn"
            } else {
                "$fornavn $etternavn"
            }

        return WordUtils.capitalizeFully(navn, ' ', '-')
    }
}
