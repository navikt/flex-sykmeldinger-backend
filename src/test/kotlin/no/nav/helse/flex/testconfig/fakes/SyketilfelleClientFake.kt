package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.syketilfelle.ErUtenforVentetidResponse
import no.nav.helse.flex.gateways.syketilfelle.FomTomPeriode
import no.nav.helse.flex.gateways.syketilfelle.SyketilfelleClient
import java.time.LocalDate

class SyketilfelleClientFake : SyketilfelleClient {
    private var erUtenforVentetid = defaultErUtenforVentetidResponse

    companion object {
        val defaultErUtenforVentetidResponse =
            ErUtenforVentetidResponse(
                erUtenforVentetid = false,
                oppfolgingsdato = LocalDate.parse("2025-01-01"),
                ventetid = FomTomPeriode(LocalDate.parse("2025-01-01"), LocalDate.parse("2025-01-20")),
            )
    }

    override fun getErUtenforVentetid(
        identer: PersonIdenter,
        sykmeldingId: String,
    ): ErUtenforVentetidResponse = erUtenforVentetid

    fun setErUtenforVentetid(utenforVentetid: ErUtenforVentetidResponse) {
        erUtenforVentetid = utenforVentetid
    }

    fun reset() {
        erUtenforVentetid = defaultErUtenforVentetidResponse
    }
}
