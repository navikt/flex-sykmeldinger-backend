package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusProducer
import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO

class SykmeldingStatusProducerFake : SykmeldingStatusProducer {
    private var sendteSykmeldinger: MutableList<SykmeldingStatusKafkaDTO> = mutableListOf()

    fun sendteSykmeldinger(): List<SykmeldingStatusKafkaDTO> = sendteSykmeldinger

    fun reset() {
        sendteSykmeldinger.clear()
    }

    override fun produserSykmeldingStatus(
        fnr: String,
        sykmelingstatusDTO: SykmeldingStatusKafkaDTO,
    ) {
        sendteSykmeldinger.add(sykmelingstatusDTO)
    }
}
