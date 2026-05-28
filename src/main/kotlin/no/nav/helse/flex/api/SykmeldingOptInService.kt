package no.nav.helse.flex.api

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.KafkaMetadataDTO
import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage
import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmelding.UgyldigOptinException
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SYKMELDINGSTATUS_LEESAH_SOURCE
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.function.Supplier

@Service
class SykmeldingOptInService(
    private val sykmeldingLeser: SykmeldingLeser,
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
    private val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    private val nowFactory: Supplier<Instant>,
) {
    private val logger = logger()

    fun behandleOptIn(
        sykmeldingId: String,
        identer: PersonIdenter,
    ) {
        val sykmelding = sykmeldingLeser.hentSykmelding(sykmeldingId = sykmeldingId, identer = identer)

        val sisteHendelse = sykmelding.sisteHendelse()
        logger.info("Opt-in: Henter sykmelding ${sykmelding.sykmeldingId} med status ${sisteHendelse.status}")
        validerOptInKanUtfores(sykmelding, sisteHendelse)

        sykepengesoknadBackendClient.opprettOptIn(lagOptInMelding(sykmelding, sisteHendelse))
        logger.info("Opt-in: Opprettet søknad for sykmelding ${sykmelding.sykmeldingId}")
    }

    private fun validerOptInKanUtfores(
        sykmelding: Sykmelding,
        sisteHendelse: SykmeldingHendelse,
    ) {
        if (sisteHendelse.status != HendelseStatus.SENDT_TIL_NAV) {
            throw UgyldigOptinException("Opt-in: Sykmeldingen ${sykmelding.sykmeldingId} har feil status ${sisteHendelse.status}")
        }

        val gyldigeArbeidssituasjoner = setOf(Arbeidssituasjon.NAERINGSDRIVENDE, Arbeidssituasjon.FRILANSER)
        val arbeidssituasjon = sisteHendelse.brukerSvar?.arbeidssituasjon?.svar
        if (arbeidssituasjon !in gyldigeArbeidssituasjoner) {
            throw UgyldigOptinException("Opt-in: Sykmeldingen ${sykmelding.sykmeldingId} har feil arbeidssituasjon $arbeidssituasjon")
        }
    }

    private fun lagOptInMelding(
        sykmelding: Sykmelding,
        sisteHendelse: SykmeldingHendelse,
    ): SykmeldingKafkaMessage =
        SykmeldingKafkaMessage(
            kafkaMetadata =
                KafkaMetadataDTO(
                    sykmeldingId = sykmelding.sykmeldingId,
                    timestamp = OffsetDateTime.ofInstant(nowFactory.get(), ZoneOffset.UTC),
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
}
