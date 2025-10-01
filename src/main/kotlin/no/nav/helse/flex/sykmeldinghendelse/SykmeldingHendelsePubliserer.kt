package no.nav.helse.flex.sykmeldinghendelse

import no.nav.helse.flex.gateways.SykmeldingStatusKafkaProducer
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingStatusHandterer.Companion.sammenstillSykmeldingStatusKafkaMessageDTO
import org.springframework.stereotype.Component

@Component
class SykmeldingHendelsePubliserer(
    private val sykmeldingStatusKafkaProducer: SykmeldingStatusKafkaProducer,
) {
    fun publiserSisteHendelse(sykmelding: Sykmelding) {
        val status =
            sammenstillSykmeldingStatusKafkaMessageDTO(
                fnr = sykmelding.pasientFnr,
                sykmeldingStatusKafkaDTO =
                    SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                        sykmeldingHendelse = sykmelding.sisteHendelse(),
                        sykmeldingId = sykmelding.sykmeldingId,
                    ),
            )
        sykmeldingStatusKafkaProducer.produserSykmeldingStatus(status)
    }
}
