package no.nav.helse.flex.api

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.sykepengesoknadbackend.SykepengesoknadBackendClient
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmelding.UgyldigOptinException
import no.nav.helse.flex.sykmeldinghendelse.Arbeidssituasjon
import no.nav.helse.flex.sykmeldinghendelse.HendelseStatus
import no.nav.helse.flex.sykmeldinghendelse.SykmeldingHendelse
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service

@Service
class SykmeldingOptInService(
    private val sykmeldingLeser: SykmeldingLeser,
    private val sykepengesoknadBackendClient: SykepengesoknadBackendClient,
    private val sykmeldingKafkaMessageKonverterer: SykmeldingKafkaMessageKonverterer,
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

        sykepengesoknadBackendClient.opprettOptIn(sykmeldingKafkaMessageKonverterer.opprettTilsvarendeSykmeldingKafkaMessage(sykmelding)!!)
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
}
