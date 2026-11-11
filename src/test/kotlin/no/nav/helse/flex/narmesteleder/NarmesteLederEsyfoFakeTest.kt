package no.nav.helse.flex.narmesteleder

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.gateways.NARMESTELEDER_ESYFO_LEESAH_TOPIC
import no.nav.helse.flex.gateways.NarmestelederEsyfoListener
import no.nav.helse.flex.gateways.pdl.FunctionalPdlError
import no.nav.helse.flex.gateways.pdl.PdlManglerNavnError
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import no.nav.helse.flex.utils.serialisertTilString
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class NarmesteLederEsyfoFakeTest : FakesTestOppsett() {
    @Autowired lateinit var listener: NarmestelederEsyfoListener

    @Autowired lateinit var pdlClient: PdlClientFake

    @Autowired lateinit var environmentToggles: EnvironmentTogglesFake

    @AfterEach
    fun `Rydd fake-data etter hver test`() {
        slettDatabase()
        pdlClient.reset()
        environmentToggles.reset()
    }

    @Nested
    inner class Innlesing {
        @Test
        fun `Tom batch endrer ikke lagrede data og kvitteres ikke`() {
            var kvitteringer = 0

            listener.listenEsyfo(emptyList()) { kvitteringer++ }

            kvitteringer shouldBe 0
            narmesteLederEsyfoRepository.count() shouldBe 0
            offset() shouldBe -1L
        }

        @Test
        fun `Offset styrer ny tabell og tombstone bevarer checkpoint`() {
            pdlClient.setFormatertNavn("Test Leder")
            val id = UUID.randomUUID()
            val aktiv = melding(id)
            var kvitteringer = 0
            listener.listenEsyfo(listOf(record(10, id, aktiv.serialisertTilString()))) { kvitteringer++ }
            narmesteLederRepository.findByNarmesteLederId(id).shouldBeNull()
            narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldNotBeNull().narmesteLederNavn shouldBe "Test Leder"

            val inaktiv = aktiv.copy(aktivTom = LocalDate.parse("2026-01-02"), timestamp = aktiv.timestamp.minusDays(1))
            listener.listenEsyfo(listOf(record(12, id, inaktiv.serialisertTilString()))) { kvitteringer++ }
            narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldNotBeNull().aktivTom shouldBe inaktiv.aktivTom
            listener.listenEsyfo(listOf(record(13, id, null))) { kvitteringer++ }
            listener.listenEsyfo(listOf(record(12, id, aktiv.serialisertTilString()))) { kvitteringer++ }
            narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldBeNull()
            listener.listenEsyfo(listOf(record(15, id, aktiv.serialisertTilString()))) { kvitteringer++ }

            narmesteLederEsyfoRepository
                .findByNarmesteLederId(id)
                .shouldNotBeNull()
                .aktivTom
                .shouldBeNull()
            kvitteringer shouldBe 5
            offset() shouldBe 15L
        }

        @Test
        fun `Checkpoint holdes separat for hver partisjon og nullstilles ved rydding`() {
            val første = UUID.randomUUID()
            val andre = UUID.randomUUID()
            listener.listenEsyfo(
                listOf(
                    record(0, første, melding(første).serialisertTilString()),
                    record(0, andre, melding(andre).serialisertTilString(), partition = 1),
                ),
            ) { }
            listener.listenEsyfo(listOf(record(1, første, null))) { }

            offset() shouldBe 1L
            offset(partition = 1) shouldBe 0L
            narmesteLederEsyfoRepository.findByNarmesteLederId(første).shouldBeNull()
            narmesteLederEsyfoRepository.findByNarmesteLederId(andre).shouldNotBeNull()

            slettDatabase()

            offset() shouldBe -1L
            offset(partition = 1) shouldBe -1L
            narmesteLederEsyfoRepository.count() shouldBe 0
        }
    }

    @Nested
    inner class PdlOppslag {
        @Test
        fun `Funksjonell PDL-feil lagrer nullable navn og lar tombstone og annen nøkkel passere`() {
            pdlClient.setFormatertNavn(FunctionalPdlError("Fant ikke person"))
            val første = UUID.randomUUID()
            val andre = UUID.randomUUID()
            var kvitteringer = 0

            listener.listenEsyfo(listOf(record(0, første, melding(første).serialisertTilString()))) { kvitteringer++ }
            narmesteLederEsyfoRepository
                .findByNarmesteLederId(første)
                .shouldNotBeNull()
                .narmesteLederNavn
                .shouldBeNull()
            pdlClient.setFormatertNavn("Test Leder")
            listener.listenEsyfo(listOf(record(1, første, null))) { kvitteringer++ }
            listener.listenEsyfo(listOf(record(2, andre, melding(andre).serialisertTilString()))) { kvitteringer++ }

            kvitteringer shouldBe 3
            narmesteLederEsyfoRepository.findByNarmesteLederId(første).shouldBeNull()
            narmesteLederEsyfoRepository.findByNarmesteLederId(andre).shouldNotBeNull().narmesteLederNavn shouldBe "Test Leder"
            offset() shouldBe 2L
        }

        @Test
        fun `Manglende PDL-navn er nullable i prod og får standardnavn i dev`() {
            pdlClient.setFormatertNavn(PdlManglerNavnError("Fant ikke navn"))
            val id = UUID.randomUUID()
            listener.listenEsyfo(listOf(record(0, id, melding(id).serialisertTilString()))) { }
            narmesteLederEsyfoRepository
                .findByNarmesteLederId(id)
                .shouldNotBeNull()
                .narmesteLederNavn
                .shouldBeNull()

            environmentToggles.setEnvironment("dev")
            val nesteId = UUID.randomUUID()
            listener.listenEsyfo(listOf(record(1, nesteId, melding(nesteId).serialisertTilString()))) { }

            narmesteLederEsyfoRepository.findByNarmesteLederId(nesteId).shouldNotBeNull().narmesteLederNavn shouldBe "Navn Navnesen"
        }
    }

    private fun record(
        offset: Long,
        id: UUID,
        payload: String?,
        partition: Int = 0,
    ) = ConsumerRecord(NARMESTELEDER_ESYFO_LEESAH_TOPIC, partition, offset, id.toString(), payload)

    private fun melding(id: UUID) = lagNarmesteLederLeesah(id, orgnummer = "123456789")

    private fun offset(partition: Int = 0) = narmesteLederEsyfoCheckpoint.lasOgHentOffset(NARMESTELEDER_ESYFO_LEESAH_TOPIC, partition)
}
