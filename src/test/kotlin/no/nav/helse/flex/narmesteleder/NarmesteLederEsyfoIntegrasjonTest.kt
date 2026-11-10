package no.nav.helse.flex.narmesteleder

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import no.nav.helse.flex.gateways.NARMESTELEDER_ESYFO_LEESAH_TOPIC
import no.nav.helse.flex.gateways.NarmestelederListener
import no.nav.helse.flex.gateways.pdl.lagGetPersonResponseData
import no.nav.helse.flex.gateways.pdl.lagGraphQlResponse
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.defaultPdlDispatcher
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.serialisertTilString
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.support.Acknowledgment
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class NarmesteLederEsyfoIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired lateinit var listener: NarmestelederListener

    @Autowired lateinit var oppdatering: OppdateringAvNarmesteLederEsyfo

    @Autowired lateinit var jdbcTemplate: JdbcTemplate

    @Autowired lateinit var pdlMockWebServer: MockWebServer

    @Autowired lateinit var environmentToggles: EnvironmentTogglesFake

    @AfterEach
    fun afterEach() {
        // eSyfo-consumeren er avslått, så både testdata og checkpoint kan slettes mellom testene.
        narmesteLederEsyfoRepository.deleteAll()
        jdbcTemplate.update("DELETE FROM narmeste_leder_esyfo_checkpoint")
        pdlMockWebServer.dispatcher = defaultPdlDispatcher
        environmentToggles.reset()
    }

    private fun record(
        offset: Long,
        id: UUID,
        payload: String?,
        partition: Int = 0,
    ) = ConsumerRecord(NARMESTELEDER_ESYFO_LEESAH_TOPIC, partition, offset, id.toString(), payload)

    private fun melding(id: UUID) = lagNarmesteLederLeesah(id, orgnummer = "123456789")

    private fun behandleEnMelding(
        record: ConsumerRecord<String, String?>,
        acknowledgment: Acknowledgment,
    ) = listener.listenEsyfo(listOf(record), acknowledgment)

    @Test
    fun `batch bevarer rekkefølge og tombstones og kvitteres én gang etter commit`() {
        val første = UUID.randomUUID()
        val andre = UUID.randomUUID()
        val inaktiv = melding(første).copy(aktivTom = LocalDate.parse("2025-01-01"))
        val annen = melding(andre).copy(aktivTom = LocalDate.parse("2025-01-01"))
        val batch =
            listOf(
                record(0, første, inaktiv.serialisertTilString(), partition = 1),
                record(0, andre, annen.serialisertTilString()),
                record(1, første, null, partition = 1),
            )
        var kvitteringer = 0

        listener.listenEsyfo(batch) {
            narmesteLederEsyfoRepository.findByNarmesteLederId(første).shouldBeNull()
            narmesteLederEsyfoRepository.findByNarmesteLederId(andre).shouldNotBeNull()
            kvitteringer++
        }
        kvitteringer shouldBe 1
        val lagret = narmesteLederEsyfoRepository.findByNarmesteLederId(andre)
        listener.listenEsyfo(batch) { kvitteringer++ }
        kvitteringer shouldBe 2
        narmesteLederEsyfoRepository.findByNarmesteLederId(andre) shouldBe lagret
    }

    @Test
    fun `feil i en batch ruller tilbake alle partisjoner uten kvittering og kan forsøkes igjen`() {
        val første = UUID.randomUUID()
        val andre = UUID.randomUUID()
        val inaktiv = melding(første).copy(aktivTom = LocalDate.parse("2025-01-01"))
        val annen = melding(andre).copy(aktivTom = LocalDate.parse("2025-01-01"))
        var kvitteringer = 0
        val feil =
            runCatching {
                listener.listenEsyfo(
                    listOf(record(0, andre, "{", partition = 1), record(0, første, inaktiv.serialisertTilString())),
                ) { kvitteringer++ }
            }.exceptionOrNull().shouldNotBeNull()
        feil.message shouldBe "ESYFO-projeksjon feilet; batchen blir forsøkt igjen"
        feil.cause.shouldBeNull()
        kvitteringer shouldBe 0
        narmesteLederEsyfoRepository.findByNarmesteLederId(første).shouldBeNull()
        jdbcTemplate.queryForObject("SELECT count(*) FROM narmeste_leder_esyfo_checkpoint", Int::class.java) shouldBe 0

        listener.listenEsyfo(
            listOf(record(0, andre, annen.serialisertTilString(), partition = 1), record(0, første, inaktiv.serialisertTilString())),
        ) { kvitteringer++ }
        kvitteringer shouldBe 1
        narmesteLederEsyfoRepository.findByNarmesteLederId(første).shouldNotBeNull()
        narmesteLederEsyfoRepository.findByNarmesteLederId(andre).shouldNotBeNull()
    }

    @Test
    fun `tom batch endrer ikke databasen og kvitteres ikke`() {
        var kvitteringer = 0
        listener.listenEsyfo(emptyList()) { kvitteringer++ }
        kvitteringer shouldBe 0
        jdbcTemplate.queryForObject("SELECT count(*) FROM narmeste_leder_esyfo_checkpoint", Int::class.java) shouldBe 0
    }

    @Test
    fun `funksjonell PDL-feil lagrer nullable navn og lar tombstone og annen nøkkel passere`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse
                    .Builder()
                    .setHeader("Content-Type", "application/json")
                    .body("""{"data":null,"errors":[]}""")
                    .build()
            }
        val første = UUID.randomUUID()
        val andre = UUID.randomUUID()
        var kvitteringer = 0

        behandleEnMelding(record(0, første, melding(første).serialisertTilString())) { kvitteringer++ }
        narmesteLederEsyfoRepository
            .findByNarmesteLederId(første)
            .shouldNotBeNull()
            .narmesteLederNavn
            .shouldBeNull()
        pdlMockWebServer.dispatcher = defaultPdlDispatcher
        behandleEnMelding(record(1, første, null)) { kvitteringer++ }
        behandleEnMelding(record(2, andre, melding(andre).serialisertTilString())) { kvitteringer++ }

        kvitteringer shouldBe 3
        narmesteLederEsyfoRepository.findByNarmesteLederId(første).shouldBeNull()
        narmesteLederEsyfoRepository.findByNarmesteLederId(andre).shouldNotBeNull().narmesteLederNavn shouldBe "Ole Gunnar"
        jdbcTemplate.queryForObject("SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint", Long::class.java) shouldBe 2L
    }

    @Test
    fun `teknisk PDL-feil lar rad og checkpoint stå urørt uten kvittering`() {
        pdlMockWebServer.dispatcher = simpleDispatcher { MockResponse.Builder().code(503).build() }
        val id = UUID.randomUUID()
        var kvitteringer = 0
        runCatching { behandleEnMelding(record(0, id, melding(id).serialisertTilString())) { kvitteringer++ } }
            .exceptionOrNull()
            .shouldNotBeNull()
            .message shouldBe "ESYFO-projeksjon feilet; batchen blir forsøkt igjen"
        kvitteringer shouldBe 0
        narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldBeNull()
        jdbcTemplate.queryForObject("SELECT count(*) FROM narmeste_leder_esyfo_checkpoint", Int::class.java) shouldBe 0

        pdlMockWebServer.dispatcher = defaultPdlDispatcher
        behandleEnMelding(record(0, id, melding(id).serialisertTilString())) { kvitteringer++ }
        kvitteringer shouldBe 1
        narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldNotBeNull()
        jdbcTemplate.queryForObject("SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint", Long::class.java) shouldBe 0L
    }

    @Test
    fun `avkrefting sletter fra eksisterende ledertabell og lagres som inaktiv i eSyfo-tabellen`() {
        val id = UUID.randomUUID()
        val aktiv = melding(id)
        val inaktiv = aktiv.copy(aktivTom = LocalDate.parse("2025-01-01"))
        var gammelKvittering = 0
        var nyKvittering = 0
        listener.listen(
            ConsumerRecord(no.nav.helse.flex.gateways.NARMESTELEDER_LEESAH_TOPIC, 0, 0, id.toString(), aktiv.serialisertTilString()),
        ) {
            gammelKvittering++
        }
        narmesteLederRepository.findByNarmesteLederId(id).shouldNotBeNull()

        behandleEnMelding(record(0, id, inaktiv.serialisertTilString())) { nyKvittering++ }
        listener.listen(
            ConsumerRecord(no.nav.helse.flex.gateways.NARMESTELEDER_LEESAH_TOPIC, 0, 1, id.toString(), inaktiv.serialisertTilString()),
        ) {
            gammelKvittering++
        }

        gammelKvittering shouldBe 2
        nyKvittering shouldBe 1
        narmesteLederRepository.findByNarmesteLederId(id).shouldBeNull()
        narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldNotBeNull().aktivTom shouldBe inaktiv.aktivTom
    }

    @Test
    fun `negativ partisjon og offset avvises før checkpoint uten kvittering`() {
        val id = UUID.randomUUID()
        var kvitteringer = 0
        for (record in listOf(record(-1, id, null), record(0, id, null, partition = -1))) {
            runCatching { behandleEnMelding(record) { kvitteringer++ } }
                .exceptionOrNull()
                .shouldNotBeNull()
                .message shouldBe "ESYFO-projeksjon feilet; batchen blir forsøkt igjen"
        }
        kvitteringer shouldBe 0
        jdbcTemplate.queryForObject("SELECT count(*) FROM narmeste_leder_esyfo_checkpoint", Int::class.java) shouldBe 0
        narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldBeNull()
    }

    @Test
    fun `tidligere upåført record for annen nøkkel behandles før neste offset`() {
        val første = UUID.randomUUID()
        val andre = UUID.randomUUID()
        val inaktivFørste = melding(første).copy(aktivTom = LocalDate.parse("2025-01-01"))
        val inaktivAndre = melding(andre).copy(aktivTom = LocalDate.parse("2025-01-01"))
        var kvitteringer = 0
        runCatching { behandleEnMelding(record(0, første, "{")) { kvitteringer++ } }.isFailure shouldBe true
        jdbcTemplate.queryForObject("SELECT count(*) FROM narmeste_leder_esyfo_checkpoint", Int::class.java) shouldBe 0
        behandleEnMelding(record(0, første, inaktivFørste.serialisertTilString())) { kvitteringer++ }
        behandleEnMelding(record(1, andre, inaktivAndre.serialisertTilString())) { kvitteringer++ }
        kvitteringer shouldBe 2
        jdbcTemplate.queryForObject("SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint", Long::class.java) shouldBe 1L
        narmesteLederEsyfoRepository.findByNarmesteLederId(første).shouldNotBeNull()
        narmesteLederEsyfoRepository.findByNarmesteLederId(andre).shouldNotBeNull()
    }

    @Test
    fun `offset styrer ny tabell og tombstone bevarer checkpoint`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher { lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Test", etternavn = "Leder")) }
        val id = UUID.randomUUID()
        val aktiv = melding(id)
        var kvitteringer = 0
        behandleEnMelding(record(10, id, aktiv.serialisertTilString())) { kvitteringer++ }
        narmesteLederRepository.findByNarmesteLederId(id).shouldBeNull()
        narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldNotBeNull().narmesteLederNavn shouldBe "Test Leder"

        val inaktiv = aktiv.copy(aktivTom = LocalDate.parse("2026-01-02"), timestamp = aktiv.timestamp.minusDays(1))
        behandleEnMelding(record(12, id, inaktiv.serialisertTilString())) { kvitteringer++ }
        narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldNotBeNull().aktivTom shouldBe inaktiv.aktivTom
        behandleEnMelding(record(13, id, null)) { kvitteringer++ }
        behandleEnMelding(record(12, id, aktiv.serialisertTilString())) { kvitteringer++ }
        narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldBeNull()
        behandleEnMelding(record(15, id, aktiv.serialisertTilString())) { kvitteringer++ }
        narmesteLederEsyfoRepository
            .findByNarmesteLederId(id)
            .shouldNotBeNull()
            .aktivTom
            .shouldBeNull()
        kvitteringer shouldBe 5
        jdbcTemplate.queryForObject("SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint", Long::class.java) shouldBe 15L
    }

    @Test
    fun `ugyldig melding rulles tilbake uten kvittering og neste record kan behandles`() {
        val id = UUID.randomUUID()
        var kvitteringer = 0
        runCatching { behandleEnMelding(record(0, id, melding(UUID.randomUUID()).serialisertTilString())) { kvitteringer++ } }
            .isFailure shouldBe true
        kvitteringer shouldBe 0
        jdbcTemplate.queryForObject("SELECT count(*) FROM narmeste_leder_esyfo_checkpoint", Int::class.java) shouldBe 0

        val inaktiv = melding(id).copy(aktivTom = LocalDate.parse("2025-01-01"))
        behandleEnMelding(record(0, id, inaktiv.serialisertTilString().dropLast(1) + ",\"status\":\"NY\",\"ekstra\":true}")) {
            kvitteringer++
        }
        kvitteringer shouldBe 1
        narmesteLederEsyfoRepository
            .findByNarmesteLederId(id)
            .shouldNotBeNull()
            .narmesteLederNavn
            .shouldBeNull()
    }

    @Test
    fun `kvitteringsfeil etter commit gir ufarlig ny levering`() {
        val id = UUID.randomUUID()
        val inaktiv = melding(id).copy(aktivTom = LocalDate.parse("2025-01-01"))
        runCatching { behandleEnMelding(record(2, id, inaktiv.serialisertTilString())) { error("Kvittering feilet") } }
            .isFailure shouldBe true
        val lagret = narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldNotBeNull()
        behandleEnMelding(record(2, id, null)) { }
        narmesteLederEsyfoRepository.findByNarmesteLederId(id) shouldBe lagret
        jdbcTemplate.queryForObject("SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint", Long::class.java) shouldBe 2L
    }

    @Test
    fun `databasefeil etter domeneskriving ruller tilbake både rad og checkpoint`() {
        val id = UUID.randomUUID()
        val inaktiv = melding(id).copy(aktivTom = LocalDate.parse("2025-01-01"))
        jdbcTemplate.execute(
            "ALTER TABLE narmeste_leder_esyfo_checkpoint ADD CONSTRAINT test_offset_grense CHECK (offset_sist_pafort <= 0)",
        )
        try {
            var kvitteringer = 0
            runCatching { behandleEnMelding(record(1, id, inaktiv.serialisertTilString())) { kvitteringer++ } }
                .isFailure shouldBe true
            kvitteringer shouldBe 0
            narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldBeNull()
            jdbcTemplate.queryForObject("SELECT count(*) FROM narmeste_leder_esyfo_checkpoint", Int::class.java) shouldBe 0
        } finally {
            jdbcTemplate.execute("ALTER TABLE narmeste_leder_esyfo_checkpoint DROP CONSTRAINT test_offset_grense")
        }
        behandleEnMelding(record(1, id, inaktiv.serialisertTilString())) { }
        narmesteLederEsyfoRepository.findByNarmesteLederId(id).shouldNotBeNull()
    }

    @Test
    fun `manglende PDL-navn er nullable i prod og får standardnavn i dev`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher {
                val data = lagGetPersonResponseData()
                lagGraphQlResponse(data.copy(hentPerson = data.hentPerson?.copy(navn = emptyList())))
            }
        val id = UUID.randomUUID()
        behandleEnMelding(record(0, id, melding(id).serialisertTilString())) { }
        narmesteLederEsyfoRepository
            .findByNarmesteLederId(id)
            .shouldNotBeNull()
            .narmesteLederNavn
            .shouldBeNull()
        environmentToggles.setEnvironment("dev")
        val nesteId = UUID.randomUUID()
        behandleEnMelding(record(1, nesteId, melding(nesteId).serialisertTilString())) { }
        narmesteLederEsyfoRepository.findByNarmesteLederId(nesteId).shouldNotBeNull().narmesteLederNavn shouldBe "Navn Navnesen"
    }

    @Test
    fun `parallell første checkpoint-innsetting serialiseres per partisjon`() {
        val id = UUID.randomUUID()
        val inaktiv = melding(id).copy(aktivTom = LocalDate.parse("2025-01-01"))
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val resultat =
                (1..2).map {
                    pool.submit<Boolean> {
                        start.await()
                        oppdatering.behandle(record(1, id, inaktiv.serialisertTilString()))
                    }
                }
            start.countDown()
            resultat.map { it.get(10, TimeUnit.SECONDS) }.sorted() shouldBe listOf(false, true)
            narmesteLederEsyfoRepository.findAll().count() shouldBe 1
        } finally {
            pool.shutdownNow()
        }
    }
}
