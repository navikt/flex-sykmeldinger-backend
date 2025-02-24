package no.nav.helse.flex.clients.ereg

interface EregClient {
    fun hentNokkelinfo(orgnummer: String): Nokkelinfo
}
