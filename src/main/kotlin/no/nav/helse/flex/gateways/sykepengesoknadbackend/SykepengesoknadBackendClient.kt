package no.nav.helse.flex.gateways.sykepengesoknadbackend

interface SykepengesoknadBackendClient {
    fun harSoknad(sykmeldingUuid: String): Boolean
}
