package no.nav.helse.flex.gateways.syketilfelle

interface SyketilfelleClient {
    fun getVentetidForSykmelding(sykmeldingId: String): VentetidForSykmeldingResponse
}
