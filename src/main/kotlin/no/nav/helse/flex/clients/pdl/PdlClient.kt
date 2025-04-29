package no.nav.helse.flex.clients.pdl

import java.time.LocalDate

interface PdlClient {
    fun hentIdenterMedHistorikk(ident: String): List<PdlIdent>

    fun hentFormattertNavn(fnr: String): String

    fun hentFoedselsdato(fnr: String): LocalDate
}
