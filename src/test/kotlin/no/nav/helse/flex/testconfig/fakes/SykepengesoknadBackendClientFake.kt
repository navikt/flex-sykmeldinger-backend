package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient

class SykepengesoknadBackendClientFake : SykepengesoknadBackendClient {
    private var harSoknadResponse = false

    override fun harSoknad(sykmeldingUuid: String): Boolean = harSoknadResponse

    fun setHarSoknad(harSoknad: Boolean) {
        harSoknadResponse = harSoknad
    }

    fun reset() {
        harSoknadResponse = false
    }
}
