package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.syketilfelle.ErUtenforVentetidResponse
import no.nav.helse.flex.gateways.syketilfelle.SammeVentetidResponse
import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient

class SyketilfelleClientFake : SyketilfelleClient {
    private var erUtenforVentetid = defaultErUtenforVentetidResponse
    private var perioderMedSammeVentetid = defaultPerioderMedSammeVentetidResponse

    companion object {
        val defaultErUtenforVentetidResponse =
            ErUtenforVentetidResponse(
                erUtenforVentetid = false,
            )

        val defaultPerioderMedSammeVentetidResponse = SammeVentetidResponse(ventetidPerioder = emptyList())
    }

    override fun getErUtenforVentetid(
        identer: PersonIdenter,
        sykmeldingId: String,
    ): ErUtenforVentetidResponse = erUtenforVentetid

    override fun getPerioderMedSammeVentetid(sykmeldingId: String): SammeVentetidResponse = perioderMedSammeVentetid

    fun setErUtenforVentetid(utenforVentetid: ErUtenforVentetidResponse) {
        erUtenforVentetid = utenforVentetid
    }

    fun setPerioderMedSammeVentetid(response: SammeVentetidResponse) {
        perioderMedSammeVentetid = response
    }

    fun reset() {
        erUtenforVentetid = defaultErUtenforVentetidResponse
        perioderMedSammeVentetid = defaultPerioderMedSammeVentetidResponse
    }
}
