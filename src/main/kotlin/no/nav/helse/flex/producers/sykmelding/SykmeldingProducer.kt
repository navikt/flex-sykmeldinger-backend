package no.nav.helse.flex.producers.sykmelding

import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Component

interface SykmeldingProducer {
    fun sendSykmelding(sykmelding: Sykmelding)
}

@Component
class SykmeldingKafkaProducer : SykmeldingProducer {
    val log = logger()

    override fun sendSykmelding(sykmelding: Sykmelding) {
        log.warn("Sykmeldingen ${sykmelding.sykmeldingId} blir ikke sendt på kafka, det er ikke implementert")
    }
}
