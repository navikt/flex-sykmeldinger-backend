package no.nav.helse.flex.narmesteleder

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.clients.pdl.FunctionalPdlError
import no.nav.helse.flex.clients.pdl.PdlClient
import no.nav.helse.flex.clients.pdl.PdlManglerNavnError
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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

        val narmestelederNavn =
            if (environmentToggles.isDevelopment()) {
                try {
                    pdlClient.hentFormattertNavn(narmesteLederLeesah.narmesteLederFnr)
                } catch (_: FunctionalPdlError) {
                    log.warn("Fant ikke navn for FNR ${narmesteLederLeesah.narmesteLederFnr} i PDL. Bruker 'Navn Navnesen'.")
                    "Navn Navnesen"
                }
            } else {
                try {
                    pdlClient.hentFormattertNavn(narmesteLederLeesah.narmesteLederFnr)
                } catch (_: PdlManglerNavnError) {
                    log.warn("Fant ikke navn for leder i nærmeste leder kobling ${narmesteLederLeesah.narmesteLederId} i PDL'.")
                    null
                }
            }

        if (narmesteLeder != null) {
            if (narmesteLederLeesah.aktivTom == null) {
                narmesteLederRepository.save(narmesteLederLeesah.tilNarmesteLeder(id = narmesteLeder.id, navn = narmestelederNavn))
                log.info("Oppdatert narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            } else {
                narmesteLederRepository.delete(narmesteLeder)
                log.info("Slettet narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            }
        } else {
            if (narmesteLederLeesah.aktivTom == null) {
                narmesteLederRepository.save(narmesteLederLeesah.tilNarmesteLeder(id = null, navn = narmestelederNavn))
                log.info("Lagret narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            } else {
                log.info("Ignorerer ny inaktiv narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            }
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
        timestamp = timestamp.toInstant(),
        oppdatert = Instant.now(),
        narmesteLederNavn = navn,
    )
