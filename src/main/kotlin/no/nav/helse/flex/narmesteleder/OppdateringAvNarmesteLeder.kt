package no.nav.helse.flex.narmesteleder

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.logger
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.pdl.PdlClient
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class OppdateringAvNarmesteLeder(
    val narmesteLederRepository: NarmesteLederRepository,
    val pdlClient: PdlClient,
) {
    val log = logger()

    fun behandleMeldingFraKafka(meldingString: String) {
        val narmesteLederLeesah = meldingString.tilNarmesteLederLeesah()
        val narmesteLeder = narmesteLederRepository.findByNarmesteLederId(narmesteLederLeesah.narmesteLederId)

        val narmestelederNavn = pdlClient.hentFormattertNavn(narmesteLederLeesah.fnr)

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
    navn: String,
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
