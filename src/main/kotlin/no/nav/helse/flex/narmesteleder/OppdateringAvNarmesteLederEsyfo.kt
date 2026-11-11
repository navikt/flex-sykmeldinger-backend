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
import tools.jackson.module.kotlin.readValue
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

    @Transactional(rollbackFor = [Exception::class])
    fun behandleBatch(records: List<ConsumerRecord<String, String?>>) {
        // Fast låserekkefølge mellom partisjoner; sorteringen bevarer record-rekkefølgen innen hver partisjon.
        records.sortedBy { it.partition() }.forEach { behandle(it) }
    }

    @Transactional(rollbackFor = [Exception::class])
    fun behandle(record: ConsumerRecord<String, String?>): Boolean {
        val sistPafort = checkpoint.lasOgHentOffset(record.topic(), record.partition())
        if (record.offset() <= sistPafort) return false

        val id =
            runCatching { UUID.fromString(record.key()) }.getOrNull()
                ?: throw IllegalArgumentException("Ugyldig nøkkel i ESYFO-record")
        require(record.key().equals(id.toString(), ignoreCase = true)) { "Ugyldig UUID-format i ESYFO-record" }
        if (record.value() == null) {
            repository.deleteByNarmesteLederId(id)
        } else {
            val melding: NarmesteLederLeesah = objectMapper.readValue(record.value()!!)
            require(melding.narmesteLederId == id) { "Nøkkel stemmer ikke med ESYFO-payload" }
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
            val eksisterende = repository.findByNarmesteLederId(id)
            repository.save(
                NarmesteLederEsyfo(
                    id = eksisterende?.id,
                    oppdatert = Instant.now(),
                    timestamp = melding.timestamp.toInstant(),
                    narmesteLederId = id,
                    orgnummer = melding.orgnummer,
                    brukerFnr = melding.fnr,
                    narmesteLederFnr = melding.narmesteLederFnr,
                    aktivFom = melding.aktivFom,
                    aktivTom = melding.aktivTom,
                    narmesteLederNavn = navn,
                ),
            )
        }
        checkpoint.lagreOffset(record.topic(), record.partition(), record.offset())
        return true
    }
}
