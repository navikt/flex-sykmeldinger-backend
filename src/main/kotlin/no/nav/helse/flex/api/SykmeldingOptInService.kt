package no.nav.helse.flex.api

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.KafkaMetadataDTO
import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage
import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmelding.UgyldigOptinException
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SYKMELDINGSTATUS_LEESAH_SOURCE
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class SykmeldingOptInService(
    private val sykmeldingLeser: SykmeldingLeser,
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
    private val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
) {
    private val logger = logger()

    fun behandleOptIn(
        sykmeldingId: String,
        identer: PersonIdenter,
    ) {
        val sykmelding = sykmeldingLeser.hentSykmelding(sykmeldingId = sykmeldingId, identer = identer)
        val sisteHendelse = sykmelding.sisteHendelse()

        logger.info("Opt-in: Henter sykmelding ${sykmelding.sykmeldingId} med status ${sisteHendelse.status}")

        if (sisteHendelse.status != HendelseStatus.SENDT_TIL_NAV) {
            throw UgyldigOptinException("Opt-in: Sykmeldingen ${sykmelding.sykmeldingId} har feil status ${sisteHendelse.status}")
        }

        if (sisteHendelse.brukerSvar?.arbeidssituasjon?.svar !in setOf(Arbeidssituasjon.NAERINGSDRIVENDE, Arbeidssituasjon.FRILANSER)) {
            throw UgyldigOptinException(
                "Opt-in: Sykmeldingen ${sykmelding.sykmeldingId} har feil arbeidssituasjon ${sisteHendelse.brukerSvar?.arbeidssituasjon?.svar}",
            )
        }

        val sykmeldingKafkaMessage =
            SykmeldingKafkaMessage(
                kafkaMetadata =
                    KafkaMetadataDTO(
                        sykmeldingId = sykmelding.sykmeldingId,
                        timestamp = OffsetDateTime.now(),
                        fnr = sykmelding.pasientFnr,
                        source = SYKMELDINGSTATUS_LEESAH_SOURCE,
                    ),
                event =
                    SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                        sykmeldingHendelse = sisteHendelse,
                        sykmeldingId = sykmelding.sykmeldingId,
                    ),
                sykmelding = sykmeldingDtoKonverterer.konverter(sykmelding),
            )

        sykepengesoknadBackendClient.opprettOptIn(sykmeldingKafkaMessage)
        logger.info("Opt-in: Opprettet søknad for sykmelding ${sykmelding.sykmeldingId}")
    }
}
