package no.nav.helse.flex.gateways.aareg

interface AaregClient {
    fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse
}
