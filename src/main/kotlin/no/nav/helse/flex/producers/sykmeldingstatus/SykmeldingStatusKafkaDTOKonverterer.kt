package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.producers.sykmeldingstatus.dto.StatusEventKafkaDTO
import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import org.springframework.stereotype.Component

@Component
class SykmeldingStatusKafkaDTOKonverterer {
    fun konverter(sykmelding: Sykmelding): SykmeldingStatusKafkaDTO =
        SykmeldingStatusKafkaDTO(
            sykmeldingId = sykmelding.sykmeldingId,
            timestamp = sykmelding.sisteStatus().opprettet.tilNorgeOffsetDateTime(),
            statusEvent =
                when (sykmelding.sisteStatus().status) {
                    HendelseStatus.APEN -> StatusEventKafkaDTO.APEN
                    HendelseStatus.AVBRUTT -> StatusEventKafkaDTO.AVBRUTT
                    HendelseStatus.SENDT_TIL_NAV -> StatusEventKafkaDTO.BEKREFTET
                    HendelseStatus.SENDT_TIL_ARBEIDSGIVER -> StatusEventKafkaDTO.SENDT
                    HendelseStatus.BEKREFTET_AVVIST -> StatusEventKafkaDTO.BEKREFTET
                    HendelseStatus.UTGATT -> StatusEventKafkaDTO.UTGATT
                },
            arbeidsgiver = null,
            sporsmals = null,
            brukerSvar = null,
            erSvarOppdatering = null,
            tidligereArbeidsgiver = null,
        )
}
