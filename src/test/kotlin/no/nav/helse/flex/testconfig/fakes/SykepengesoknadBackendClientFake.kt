package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage

class SykepengesoknadBackendClientFake : SykepengesoknadBackendClient {
    val opprettOptInRequests = mutableListOf<SykmeldingKafkaMessage>()

    override fun opprettOptIn(sykmeldingKafkaMessage: SykmeldingKafkaMessage) {
        opprettOptInRequests.add(sykmeldingKafkaMessage)
    }

    fun antallOpprettOptInKall(): Int = opprettOptInRequests.size

    fun reset() {
        opprettOptInRequests.clear()
    }
}
