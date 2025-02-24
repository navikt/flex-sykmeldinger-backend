package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.clients.aareg.ArbeidsforholdoversiktResponse

class AaregClientFake : AaregClient {
    private val arbeidsforholdOversikter: MutableMap<String?, Result<ArbeidsforholdoversiktResponse>> = mutableMapOf()

    fun setArbeidsforholdoversikt(
        arbeidsforhold: ArbeidsforholdoversiktResponse,
        fnr: String? = null,
    ) {
        arbeidsforholdOversikter[fnr] = Result.success(arbeidsforhold)
    }

    fun setArbeidsforholdoversikt(
        failure: Exception,
        fnr: String? = null,
    ) {
        arbeidsforholdOversikter[fnr] = Result.failure(failure)
    }

    fun reset() {
        arbeidsforholdOversikter.clear()
    }

    override fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse {
        val value =
            arbeidsforholdOversikter.getOrElse(fnr) {
                arbeidsforholdOversikter.getOrElse(null) {
                    throw RuntimeException("Fant ikke arbeidsforholdoversikt for $fnr")
                }
            }
        return value.getOrThrow()
    }
}
