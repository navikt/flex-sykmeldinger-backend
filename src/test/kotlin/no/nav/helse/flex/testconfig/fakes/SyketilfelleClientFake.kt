package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.gateways.syketilfelle.VentetidForSykmeldingResponse

class SyketilfelleClientFake : SyketilfelleClient {
    private var ventetidForSykmelding = defaultVentetidForSykmeldingResponse

    companion object {
        val defaultVentetidForSykmeldingResponse =
            VentetidForSykmeldingResponse(
                erUtenforVentetid = false,
                periodeMedSammeVentetid = emptyList(),
            )
    }

    override fun getVentetidForSykmelding(sykmeldingId: String): VentetidForSykmeldingResponse = ventetidForSykmelding

    fun setVentetidForSykmelding(response: VentetidForSykmeldingResponse) {
        ventetidForSykmelding = response
    }

    fun reset() {
        ventetidForSykmelding = defaultVentetidForSykmeldingResponse
    }
}
