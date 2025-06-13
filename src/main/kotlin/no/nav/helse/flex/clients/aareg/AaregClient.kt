package no.nav.helse.flex.clients.aareg

interface AaregClient {
    fun getArbeidstakerArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse

    fun getArbeidsstedArbeidsforholdoversikt(arbeidsstedOrgnummer: String): ArbeidsforholdoversiktResponse
}
