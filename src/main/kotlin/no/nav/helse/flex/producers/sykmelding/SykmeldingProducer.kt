package no.nav.helse.flex.producers.sykmelding

import no.nav.helse.flex.sykmelding.domain.Sykmelding
import org.springframework.stereotype.Component

interface SykmeldingProducer {
    fun sendSykmelding(sykmelding: Sykmelding)
}

@Component
class SykmeldingKafkaProducer : SykmeldingProducer {
    override fun sendSykmelding(sykmelding: Sykmelding): Unit = throw NotImplementedError("sendSykmelding is not implemented")
}
