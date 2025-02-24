package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.clients.aareg.ArbeidsforholdoversiktResponse

class AaregClientFake : AaregClient {
    private val arbeidsforholdOversikter: MutableMap<String, ArbeidsforholdoversiktResponse> = mutableMapOf()

    fun setArbeidsforholdoversikt(
        fnr: String,
        arbeidsforhold: ArbeidsforholdoversiktResponse,
    ) {
        arbeidsforholdOversikter[fnr] = arbeidsforhold
    }

    fun reset() {
        arbeidsforholdOversikter.clear()
    }

    override fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse =
        arbeidsforholdOversikter.getOrElse(fnr) {
            throw RuntimeException("Fant ikke arbeidsforholdoversikt for $fnr")
        }
}
