package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.gateways.pdl.FunctionalPdlError
import no.nav.helse.flex.gateways.pdl.PdlClient
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederEsyfo
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.objectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.DeserializationFeature
import java.time.Instant
import java.util.UUID

@Component
class OppdateringAvNarmesteLederEsyfo(
    private val repository: NarmesteLederEsyfoRepository,
    private val checkpoint: NarmesteLederEsyfoCheckpoint,
    private val pdlClient: PdlClient,
    private val environmentToggles: EnvironmentToggles,
) {
    private val log = logger()
    private val meldingsleser =
        objectMapper
            .readerFor(
                NarmesteLederLeesah::class.java,
            ).without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    @Transactional(rollbackFor = [Exception::class])
    fun behandleBatch(records: List<ConsumerRecord<String, String?>>) {
        records.forEach { behandle(it) }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun behandle(record: ConsumerRecord<String, String?>): Boolean {
        val sistBehandlet = checkpoint.lasOgHentOffset(record.topic(), record.partition())
        if (record.offset() <= sistBehandlet) return false

        val id = UUID.fromString(record.key())
        val verdi = record.value()
        if (verdi == null) {
            repository.deleteByNarmesteLederId(id)
        } else {
            val melding = meldingsleser.readValue<NarmesteLederLeesah>(verdi)
            require(melding.narmesteLederId == id) { "Nøkkel stemmer ikke med ESYFO-payload" }
            lagre(melding)
        }
        checkpoint.lagreOffset(record.topic(), record.partition(), record.offset())
        return true
    }

    private fun lagre(melding: NarmesteLederLeesah) {
        require(melding.orgnummer.matches(Regex("[0-9]{9}"))) { "Ugyldig orgnummer i ESYFO-payload" }
        val navn =
            if (melding.aktivTom != null) {
                null
            } else {
                try {
                    pdlClient.hentFormattertNavn(melding.narmesteLederFnr)
                } catch (_: FunctionalPdlError) {
                    log.warn("Funksjonell PDL-feil ved navneoppslag for ESYFO-projeksjon")
                    if (environmentToggles.isDevelopment()) "Navn Navnesen" else null
                }
            }
        val eksisterende = repository.findByNarmesteLederId(melding.narmesteLederId)
        repository.save(
            NarmesteLederEsyfo(
                id = eksisterende?.id,
                oppdatert = Instant.now(),
                timestamp = melding.timestamp.toInstant(),
                narmesteLederId = melding.narmesteLederId,
                orgnummer = melding.orgnummer,
                brukerFnr = melding.fnr,
                narmesteLederFnr = melding.narmesteLederFnr,
                aktivFom = melding.aktivFom,
                aktivTom = melding.aktivTom,
                narmesteLederNavn = navn,
            ),
        )
    }
}
