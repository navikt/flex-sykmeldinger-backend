package no.nav.helse.flex.clients.pdl

interface PdlClient {
    fun hentIdenterMedHistorikk(ident: String): List<PdlIdent>

    fun hentFormattertNavn(fnr: String): String
}
