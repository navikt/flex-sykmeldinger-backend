package no.nav.helse.flex.gateways

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.narmesteleder.lagNarmesteLederLeesah
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.utils.serialisertTilString
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class NarmesteLederKafkaListenerFakeTest : FakesTestOppsett() {
    @Autowired
    lateinit var listener: NarmestelederListener

    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @Test
    fun `tombstone kvitteres uten oppdatering og ukjent status hindrer ikke neste melding`() {
        val tombstoneId = UUID.randomUUID()
        val lederMedUkjentStatus = lagNarmesteLederLeesah(UUID.randomUUID())
        val nesteLeder = lagNarmesteLederLeesah(UUID.randomUUID())
        val meldingMedUkjentStatus = lederMedUkjentStatus.serialisertTilString().dropLast(1) + ",\"status\":\"NOE_NYTT\"}"
        var kvitteringer = 0

        listener.listen(ConsumerRecord(NARMESTELEDER_LEESAH_TOPIC, 0, 0, tombstoneId.toString(), null)) { kvitteringer++ }
        kvitteringer shouldBe 1
        narmesteLederRepository.findByNarmesteLederId(tombstoneId).shouldBeNull()

        listener.listen(
            ConsumerRecord(NARMESTELEDER_LEESAH_TOPIC, 0, 1, lederMedUkjentStatus.narmesteLederId.toString(), meldingMedUkjentStatus),
        ) {
            kvitteringer++
        }
        kvitteringer shouldBe 2
        narmesteLederRepository.findByNarmesteLederId(lederMedUkjentStatus.narmesteLederId).shouldNotBeNull()

        listener.listen(
            ConsumerRecord(NARMESTELEDER_LEESAH_TOPIC, 0, 2, nesteLeder.narmesteLederId.toString(), nesteLeder.serialisertTilString()),
        ) {
            kvitteringer++
        }
        kvitteringer shouldBe 3
        narmesteLederRepository.findByNarmesteLederId(nesteLeder.narmesteLederId).shouldNotBeNull()
    }

    @Test
    fun `meldinger fra team sykmelding oppdaterer aktiv rad også ved eldre tidsstempel`() {
        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
        val eldreMelding = melding.copy(timestamp = melding.timestamp.minusDays(1), orgnummer = "888888")
        var kvitteringer = 0

        listener.listen(
            ConsumerRecord(NARMESTELEDER_LEESAH_TOPIC, 0, 0, melding.narmesteLederId.toString(), melding.serialisertTilString()),
        ) { kvitteringer++ }
        val lagret = narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldNotBeNull()
        listener.listen(
            ConsumerRecord(NARMESTELEDER_LEESAH_TOPIC, 0, 1, melding.narmesteLederId.toString(), eldreMelding.serialisertTilString()),
        ) { kvitteringer++ }
        kvitteringer shouldBe 2
        narmesteLederRepository.findAll().count { it.narmesteLederId == melding.narmesteLederId } shouldBe 1
        val oppdatert = narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldNotBeNull()
        oppdatert.id shouldBe lagret.id
        oppdatert.orgnummer shouldBe eldreMelding.orgnummer
    }
}
