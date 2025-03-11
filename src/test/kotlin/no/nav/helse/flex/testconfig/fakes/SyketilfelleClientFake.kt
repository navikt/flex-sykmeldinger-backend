package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.clients.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.config.PersonIdenter

class SyketilfelleClientFake : SyketilfelleClient {
    private var erUtenforVentetid: Boolean = false

    override fun getErUtenforVentetid(
        identer: PersonIdenter,
        sykmeldingId: String,
    ): Boolean = erUtenforVentetid

    fun setErUtenforVentetid(utenforVentetid: Boolean) {
        erUtenforVentetid = utenforVentetid
    }

    fun reset() {
        erUtenforVentetid = false
    }
}
