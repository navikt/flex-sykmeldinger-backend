package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.gateways.pdl.FunctionalPdlError
import no.nav.helse.flex.gateways.pdl.PdlClient
import no.nav.helse.flex.gateways.pdl.PdlManglerNavnError
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.module.kotlin.readValue
import java.time.Instant

@Component
class OppdateringAvNarmesteLeder(
    val narmesteLederRepository: NarmesteLederRepository,
    val pdlClient: PdlClient,
    private val environmentToggles: EnvironmentToggles,
) {
    val log = logger()

    @Transactional(rollbackFor = [Exception::class])
    fun behandleMeldingFraKafka(meldingString: String) {
        val narmesteLederLeesah = meldingString.tilNarmesteLederLeesah()
        val narmesteLeder = narmesteLederRepository.findByNarmesteLederId(narmesteLederLeesah.narmesteLederId)

        if (narmesteLederLeesah.aktivTom != null) {
            if (narmesteLeder != null) {
                narmesteLederRepository.delete(narmesteLeder)
                log.info("Slettet narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            } else {
                log.info("Ignorerer ny inaktiv narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            }
            return
        }

        val narmestelederNavn =
            if (environmentToggles.isDevelopment()) {
                try {
                    pdlClient.hentFormattertNavn(narmesteLederLeesah.narmesteLederFnr)
                } catch (_: FunctionalPdlError) {
                    log.warn("Fant ikke navn for narmesteleder i PDL. Bruker standardnavn.")
                    "Navn Navnesen"
                }
            } else {
                try {
                    pdlClient.hentFormattertNavn(narmesteLederLeesah.narmesteLederFnr)
                } catch (_: PdlManglerNavnError) {
                    log.warn("Fant ikke navn for leder i nærmeste leder kobling ${narmesteLederLeesah.narmesteLederId} i PDL.")
                    null
                }
            }

        narmesteLederRepository.save(narmesteLederLeesah.tilNarmesteLeder(id = narmesteLeder?.id, navn = narmestelederNavn))
        if (narmesteLeder != null) {
            log.info("Oppdatert narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
        } else {
            log.info("Lagret narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
        }
    }

    fun String.tilNarmesteLederLeesah(): NarmesteLederLeesah = objectMapper.readValue(this)
}

private fun NarmesteLederLeesah.tilNarmesteLeder(
    id: String?,
    navn: String?,
): NarmesteLeder =
    NarmesteLeder(
        id = id,
        narmesteLederId = narmesteLederId,
        brukerFnr = fnr,
        orgnummer = orgnummer,
        narmesteLederFnr = narmesteLederFnr,
        aktivFom = aktivFom,
        aktivTom = aktivTom,
        timestamp = timestamp.toInstant(),
        oppdatert = Instant.now(),
        narmesteLederNavn = navn,
    )
