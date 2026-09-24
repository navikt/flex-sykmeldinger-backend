package no.nav.helse.flex.api

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient
import no.nav.helse.flex.optin.OptIn
import no.nav.helse.flex.optin.OptInDbRecord
import no.nav.helse.flex.optin.OptInDbRepository
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmelding.UgyldigOptinException
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.function.Supplier

@Service
class SykmeldingOptInService(
    private val sykmeldingLeser: SykmeldingLeser,
    private val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    private val sykmeldingKafkaMessageKonverterer: SykmeldingKafkaMessageKonverterer,
    private val optInDbRepository: OptInDbRepository,
    private val nowFactory: Supplier<Instant>,
) {
    private val logger = logger()

    @Transactional(rollbackFor = [Exception::class])
    fun behandleOptIn(
        sykmeldingId: String,
        identer: PersonIdenter,
    ) {
        val sykmelding = sykmeldingLeser.hentSykmelding(sykmeldingId = sykmeldingId, identer = identer)

        val sisteHendelse = sykmelding.sisteHendelse()
        logger.info("Opt-in: Henter sykmelding ${sykmelding.sykmeldingId} med status ${sisteHendelse.status}")
        validerOptInKanUtfores(sykmelding, sisteHendelse)

        optInDbRepository.save(
            OptInDbRecord(
                sykmeldingId = sykmelding.sykmeldingId,
                opprettet = nowFactory.get(),
            ),
        )
        sykepengesoknadBackendClient.opprettOptIn(sykmeldingKafkaMessageKonverterer.opprettTilsvarendeSykmeldingKafkaMessage(sykmelding)!!)
        logger.info("Opt-in: Opprettet søknad for sykmelding ${sykmelding.sykmeldingId}")
    }

    fun hentOptIn(sykmeldingId: String): List<OptIn> = optInDbRepository.findAllBySykmeldingId(sykmeldingId).map { it.tilOptIn() }

    fun hentOptInPerSykmelding(sykmeldingIder: Collection<String>): Map<String, List<OptIn>> {
        if (sykmeldingIder.isEmpty()) {
            return emptyMap()
        }
        return optInDbRepository
            .findAllBySykmeldingIdIn(sykmeldingIder)
            .map { it.tilOptIn() }
            .groupBy { it.sykmeldingId }
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
}
