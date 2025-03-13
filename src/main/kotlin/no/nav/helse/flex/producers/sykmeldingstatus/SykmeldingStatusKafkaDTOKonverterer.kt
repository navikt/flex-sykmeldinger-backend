package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.domain.*

class SykmeldingStatusKafkaDTOKonverterer(
    private val brukerSvarKonverterer: BrukerSvarKafkaDTOKonverterer = BrukerSvarKafkaDTOKonverterer(),
    private val sporsmalsKafkaDTOKonverterer: SporsmalsKafkaDTOKonverterer = SporsmalsKafkaDTOKonverterer(),
) {
    fun konverter(sykmelding: Sykmelding): SykmeldingStatusKafkaDTO {
        val sisteHendelse = sykmelding.sisteStatus()
        val brukerSvar = sisteHendelse.sporsmalSvar?.let(brukerSvarKonverterer::konverterTilBrukerSvar)
        val sporsmols =
            brukerSvar?.let {
                sporsmalsKafkaDTOKonverterer.konverterTilSporsmals(
                    it,
                    sisteHendelse.arbeidstakerInfo,
                    sykmeldingId = sykmelding.sykmeldingId,
                )
            }
        return SykmeldingStatusKafkaDTO(
            sykmeldingId = sykmelding.sykmeldingId,
            timestamp = sisteHendelse.opprettet.tilNorgeOffsetDateTime(),
            statusEvent = konverterStatusEvent(sykmelding.sisteStatus().status),
            arbeidsgiver = sisteHendelse.arbeidstakerInfo?.let(::konverterArbeidsgiver),
            // For bakoverkompatabilitet. Consumere burde bruke `brukerSvar`
            sporsmals = sporsmols,
            brukerSvar = brukerSvar,
            erSvarOppdatering = null,
            tidligereArbeidsgiver = null,
        )
    }

    internal fun konverterStatusEvent(hendelseStatus: HendelseStatus): String =
        when (hendelseStatus) {
            HendelseStatus.APEN -> StatusEventKafkaDTO.APEN
            HendelseStatus.AVBRUTT -> StatusEventKafkaDTO.AVBRUTT
            HendelseStatus.SENDT_TIL_NAV -> StatusEventKafkaDTO.BEKREFTET
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER -> StatusEventKafkaDTO.SENDT
            HendelseStatus.BEKREFTET_AVVIST -> StatusEventKafkaDTO.BEKREFTET
            HendelseStatus.UTGATT -> StatusEventKafkaDTO.UTGATT
        }

    internal fun konverterArbeidsgiver(arbeidstakerInfo: ArbeidstakerInfo): ArbeidsgiverStatusKafkaDTO {
        val arbeidsgiver = arbeidstakerInfo.arbeidsgiver
        return ArbeidsgiverStatusKafkaDTO(
            orgnummer = arbeidsgiver.orgnummer,
            juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
            orgNavn = arbeidsgiver.orgnavn,
        )
    }
}
