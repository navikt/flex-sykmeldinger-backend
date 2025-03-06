package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.producers.sykmelding.SykmeldingProducer
import no.nav.helse.flex.sykmelding.domain.Sykmelding

class SykmeldingProducerFake : SykmeldingProducer {
    private var sendteSykmeldinger: MutableList<Sykmelding> = mutableListOf()

    fun sendteSykmeldinger(): List<Sykmelding> = sendteSykmeldinger

    fun reset() {
        sendteSykmeldinger.clear()
    }

    override fun sendSykmelding(sykmelding: Sykmelding) {
        sendteSykmeldinger.add(sykmelding)
    }
}
