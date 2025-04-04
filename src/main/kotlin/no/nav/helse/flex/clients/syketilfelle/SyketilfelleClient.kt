package no.nav.helse.flex.clients.syketilfelle

import no.nav.helse.flex.config.PersonIdenter

interface SyketilfelleClient {
    fun getErUtenforVentetid(
        identer: PersonIdenter,
        sykmeldingId: String,
    ): ErUtenforVentetidResponse
}
