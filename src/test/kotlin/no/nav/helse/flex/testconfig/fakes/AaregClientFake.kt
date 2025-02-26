package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.clients.aareg.ArbeidsforholdoversiktResponse

class AaregClientFake : AaregClient {
    private val arbeidsforholdOversikter: MutableMap<String, Result<ArbeidsforholdoversiktResponse>> = mutableMapOf()

    companion object {
        val defaultArbeidsforholdoversiktResponse = lagArbeidsforholdOversiktResponse(arbeidsforholdoversikter = emptyList())
    }

    fun setArbeidsforholdoversikt(
        arbeidsforhold: ArbeidsforholdoversiktResponse,
        fnr: String = "__accept_any_fnr",
    ) {
        arbeidsforholdOversikter[fnr] = Result.success(arbeidsforhold)
    }

    fun setArbeidsforholdoversikt(
        failure: Exception,
        fnr: String = "__accept_any_fnr",
    ) {
        arbeidsforholdOversikter[fnr] = Result.failure(failure)
    }

    fun reset() {
        arbeidsforholdOversikter.clear()
    }

    override fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse {
        if (arbeidsforholdOversikter.isEmpty()) {
            return defaultArbeidsforholdoversiktResponse
        }
        val value =
            arbeidsforholdOversikter[fnr] ?: arbeidsforholdOversikter["__accept_any_fnr"]
                ?: throw IllegalStateException("No response found for $fnr")
        return value.getOrThrow()
    }
}
