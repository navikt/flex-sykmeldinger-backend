package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.producers.sykmeldingstatus.SykmeldingStatusProducer

class SykmeldingStatusProducerFake : SykmeldingStatusProducer {
    private var sendteStatuser: MutableList<SykmeldingStatusKafkaMessageDTO> = mutableListOf()

    fun sendteSykmeldinger(): List<SykmeldingStatusKafkaMessageDTO> = sendteStatuser

    fun reset() {
        sendteStatuser.clear()
    }

    override fun produserSykmeldingStatus(sykmeldingStatusKafkaMessageDTO: SykmeldingStatusKafkaMessageDTO): Boolean {
        sendteStatuser.add(sykmeldingStatusKafkaMessageDTO)
        return true
    }
}
