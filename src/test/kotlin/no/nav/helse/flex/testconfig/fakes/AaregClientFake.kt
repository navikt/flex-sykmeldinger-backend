package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.aareg.AaregClient
import no.nav.helse.flex.clients.aareg.ArbeidsforholdoversiktResponse

class AaregClientFake : AaregClient {
    private val arbeidstakerArbeidsforholdOversikter =
        FakeResponseCollection(
            defaultResponse = { lagArbeidsforholdOversiktResponse(arbeidsforholdoversikter = emptyList()) },
        )

    private val arbeidsstedArbeidsforholdOversikter =
        FakeResponseCollection(
            defaultResponse = { lagArbeidsforholdOversiktResponse(arbeidsforholdoversikter = emptyList()) },
        )

    fun setArbeidsforholdoversikt(
        arbeidsforhold: ArbeidsforholdoversiktResponse,
        fnr: String = FakeResponseCollection.DEFAULT_ACCEPT_ANY_PARAM,
    ) {
        arbeidstakerArbeidsforholdOversikter.setSuccessResponse(response = arbeidsforhold, requestParam = fnr)
    }

    fun setArbeidsforholdoversikt(
        failure: Exception,
        fnr: String = FakeResponseCollection.DEFAULT_ACCEPT_ANY_PARAM,
    ) {
        arbeidstakerArbeidsforholdOversikter.setFailureResponse(failure = failure, requestParam = fnr)
    }

    fun setArbeidsstedArbeidsforholdoversikt(
        arbeidsforhold: ArbeidsforholdoversiktResponse,
        arbeidsstedOrgnummer: String = FakeResponseCollection.DEFAULT_ACCEPT_ANY_PARAM,
    ) {
        arbeidsstedArbeidsforholdOversikter.setSuccessResponse(
            response = arbeidsforhold,
            requestParam = arbeidsstedOrgnummer,
        )
    }

    fun setArbeidsstedArbeidsforholdoversikt(
        failure: Exception,
        arbeidsstedOrgnummer: String = FakeResponseCollection.DEFAULT_ACCEPT_ANY_PARAM,
    ) {
        arbeidsstedArbeidsforholdOversikter.setFailureResponse(
            failure = failure,
            requestParam = arbeidsstedOrgnummer,
        )
    }

    fun reset() {
        arbeidstakerArbeidsforholdOversikter.reset()
        arbeidsstedArbeidsforholdOversikter.reset()
    }

    override fun getArbeidstakerArbeidsforholdoversikt(fnr: String): ArbeidsforholdoversiktResponse =
        arbeidstakerArbeidsforholdOversikter.getResponseOrThrowFor(fnr)

    override fun getArbeidsstedArbeidsforholdoversikt(arbeidsstedOrgnummer: String): ArbeidsforholdoversiktResponse =
        arbeidsstedArbeidsforholdOversikter.getResponseOrThrowFor(arbeidsstedOrgnummer)
}

private class FakeResponseCollection<RES>(
    val defaultResponse: () -> RES,
) {
    companion object {
        const val DEFAULT_ACCEPT_ANY_PARAM = "__accept_any_param"
    }

    private val responsesByParam: MutableMap<String, Result<RES>> = mutableMapOf()

    fun setSuccessResponse(
        response: RES,
        requestParam: String = DEFAULT_ACCEPT_ANY_PARAM,
    ) {
        responsesByParam[requestParam] = Result.success(response)
    }

    fun setFailureResponse(
        failure: Exception,
        requestParam: String = DEFAULT_ACCEPT_ANY_PARAM,
    ) {
        responsesByParam[requestParam] = Result.failure(failure)
    }

    fun getResponseOrThrowFor(requestParam: String): RES =
        when {
            responsesByParam.isEmpty() -> defaultResponse()
            requestParam in responsesByParam -> responsesByParam[requestParam]!!.getOrThrow()
            DEFAULT_ACCEPT_ANY_PARAM in responsesByParam -> responsesByParam[DEFAULT_ACCEPT_ANY_PARAM]!!.getOrThrow()
            else -> throw IllegalStateException("No response found for $requestParam")
        }

    fun reset() {
        responsesByParam.clear()
    }
}
