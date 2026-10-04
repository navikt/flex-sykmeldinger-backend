package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.syketilfelle.ErUtenforVentetidResponse
import no.nav.helse.flex.gateways.syketilfelle.SammeVentetidResponse
import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.gateways.syketilfelle.VentetidForSykmeldingResponse

class SyketilfelleClientFake : SyketilfelleClient {
    private var erUtenforVentetid = defaultErUtenforVentetidResponse
    private var perioderMedSammeVentetid = defaultPerioderMedSammeVentetidResponse
    private var ventetidForSykmelding = defaultVentetidForSykmeldingResponse

    companion object {
        val defaultErUtenforVentetidResponse =
            ErUtenforVentetidResponse(
                erUtenforVentetid = false,
            )

        val defaultPerioderMedSammeVentetidResponse = SammeVentetidResponse(ventetidPerioder = emptyList())

        val defaultVentetidForSykmeldingResponse =
            VentetidForSykmeldingResponse(
                erUtenforVentetid = false,
                periodeMedSammeVentetid = emptyList(),
            )
    }

    override fun getErUtenforVentetid(
        identer: PersonIdenter,
        sykmeldingId: String,
    ): ErUtenforVentetidResponse = erUtenforVentetid

    override fun getPerioderMedSammeVentetid(sykmeldingId: String): SammeVentetidResponse = perioderMedSammeVentetid

    override fun getVentetidForSykmelding(sykmeldingId: String): VentetidForSykmeldingResponse = ventetidForSykmelding

    fun setErUtenforVentetid(utenforVentetid: ErUtenforVentetidResponse) {
        erUtenforVentetid = utenforVentetid
    }

    fun setPerioderMedSammeVentetid(response: SammeVentetidResponse) {
        perioderMedSammeVentetid = response
    }

    fun setVentetidForSykmelding(response: VentetidForSykmeldingResponse) {
        ventetidForSykmelding = response
    }

    fun reset() {
        erUtenforVentetid = defaultErUtenforVentetidResponse
        perioderMedSammeVentetid = defaultPerioderMedSammeVentetidResponse
        ventetidForSykmelding = defaultVentetidForSykmeldingResponse
    }
}
