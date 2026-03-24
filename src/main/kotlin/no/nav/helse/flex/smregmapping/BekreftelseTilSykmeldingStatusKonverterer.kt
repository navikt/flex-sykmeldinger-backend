package no.nav.helse.flex.smregmapping

import no.nav.helse.flex.sykmeldingbekreftelse.BekreftelseDto
import no.nav.helse.flex.sykmeldingbekreftelse.BekreftelseKonverterer
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO

object BekreftelseTilSykmeldingStatusKonverterer {
    fun konverterTilSykmeldingStatusEvent(
        bekreftelse: BekreftelseDto,
        sykmeldingId: String,
    ): SykmeldingStatusKafkaDTO {
        val sykmeldingHendelse = BekreftelseKonverterer.konverterTilSykmeldingHendelse(bekreftelse = bekreftelse)
        val statusEvent =
            SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                sykmeldingHendelse = sykmeldingHendelse,
                sykmeldingId = sykmeldingId,
            )
        return statusEvent
    }
}
