package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage

class SykepengesoknadBackendClientFake : SykepengesoknadBackendClient {
    val opprettOptInRequests = mutableListOf<SykmeldingKafkaMessage>()
    private var harSoknadResponse: Boolean = false

    override fun harSoknad(sykmeldingId: String): Boolean = harSoknadResponse

    fun setHarSoknad(verdi: Boolean) {
        harSoknadResponse = verdi
    }

    override fun opprettOptIn(sykmeldingKafkaMessage: SykmeldingKafkaMessage) {
        opprettOptInRequests.add(sykmeldingKafkaMessage)
    }

    fun antallOpprettOptInKall(): Int = opprettOptInRequests.size

    fun reset() {
        opprettOptInRequests.clear()
        harSoknadResponse = false
    }
}
