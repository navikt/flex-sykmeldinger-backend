package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.clients.aareg.ArbeidsforholdoversiktResponse

class AaregClientFake : AaregClient {
    private val arbeidsforholdOversikter: MutableMap<String, Result<ArbeidsforholdoversiktResponse>> = mutableMapOf()

    init {
        reset()
    }

    companion object {
        val defaultArbeidsforholdoversiktResponse = lagArbeidsforholdOversiktResponse(arbeidsforholdoversikter = emptyList())
    }

    fun setArbeidsforholdoversikt(
        arbeidsforhold: ArbeidsforholdoversiktResponse,
        fnr: String = "__default",
    ) {
        arbeidsforholdOversikter[fnr] = Result.success(arbeidsforhold)
    }

    fun setArbeidsforholdoversikt(
        failure: Exception,
        fnr: String = "__default",
    ) {
        arbeidsforholdOversikter[fnr] = Result.failure(failure)
    }

    fun reset() {
        arbeidsforholdOversikter.clear()
        setArbeidsforholdoversikt(defaultArbeidsforholdoversiktResponse)
    }

    override fun getArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse {
        val value = arbeidsforholdOversikter[fnr] ?: arbeidsforholdOversikter["__default"]!!
        return value.getOrThrow()
    }
}
