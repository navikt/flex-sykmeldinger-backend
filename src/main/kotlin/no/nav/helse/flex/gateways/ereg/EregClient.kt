package no.nav.helse.flex.gateways.ereg

interface EregClient {
    fun hentNokkelinfo(orgnummer: String): Nokkelinfo
}
