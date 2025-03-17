package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.domain.*

class SykmeldingStatusKafkaDTOKonverterer(
    private val brukerSvarKonverterer: BrukerSvarKafkaDTOKonverterer = BrukerSvarKafkaDTOKonverterer(),
    private val sporsmalsKafkaDTOKonverterer: SporsmalsKafkaDTOKonverterer = SporsmalsKafkaDTOKonverterer(),
) {
    fun konverter(sykmelding: Sykmelding): SykmeldingStatusKafkaDTO {
        val sisteHendelse = sykmelding.sisteHendelse()
        val brukerSvar = sisteHendelse.sporsmalSvar?.let(brukerSvarKonverterer::konverterTilBrukerSvar)
        val sporsmols =
            brukerSvar?.let {
                sporsmalsKafkaDTOKonverterer.konverterTilSporsmals(
                    brukerSvar = it,
                    arbeidstakerInfo = sisteHendelse.arbeidstakerInfo,
                    sykmeldingId = sykmelding.sykmeldingId,
                )
            }
        return SykmeldingStatusKafkaDTO(
            sykmeldingId = sykmelding.sykmeldingId,
            timestamp = sisteHendelse.opprettet.tilNorgeOffsetDateTime(),
            statusEvent = konverterStatusEvent(sykmelding.sisteHendelse().status),
            arbeidsgiver = sisteHendelse.arbeidstakerInfo?.let(::konverterArbeidsgiver),
            // For bakoverkompatabilitet. Consumere burde bruke `brukerSvar`
            sporsmals = sporsmols,
            brukerSvar = brukerSvar,
            // TODO: Legg til når tidligereArbeidsgiver ligger i sykmeldingHendelse
            tidligereArbeidsgiver = null,
            // Dette ser ut til å bli ignorert
            erSvarOppdatering = null,
        )
    }

    internal fun konverterStatusEvent(hendelseStatus: HendelseStatus): String {
        // TODO: Lurer på om dette er feil, se neders her: https://github.com/navikt/sykmeldinger-backend/blob/f50854794f617de634cc7972bfd4764983a3c20e/src/main/kotlin/no/nav/syfo/sykmeldingstatus/SykmeldingStatusService.kt#L392
        return when (hendelseStatus) {
            HendelseStatus.APEN -> StatusEventKafkaDTO.APEN
            HendelseStatus.AVBRUTT -> StatusEventKafkaDTO.AVBRUTT
            HendelseStatus.SENDT_TIL_NAV -> StatusEventKafkaDTO.BEKREFTET
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER -> StatusEventKafkaDTO.SENDT
            HendelseStatus.BEKREFTET_AVVIST -> StatusEventKafkaDTO.BEKREFTET
            HendelseStatus.UTGATT -> StatusEventKafkaDTO.UTGATT
        }
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
