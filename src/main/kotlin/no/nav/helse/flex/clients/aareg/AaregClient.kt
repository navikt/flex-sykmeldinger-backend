package no.nav.helse.flex.clients.aareg

interface AaregClient {
    fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse
}
