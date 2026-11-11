package no.nav.helse.flex.gateways

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import mockwebserver3.MockWebServer
import no.nav.helse.flex.gateways.pdl.lagGetPersonResponseData
import no.nav.helse.flex.gateways.pdl.lagGraphQlResponse
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import no.nav.helse.flex.narmesteleder.lagNarmesteLederLeesah
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.defaultPdlDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.serialisertTilString
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class NarmesteLederKafkaListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    @Qualifier("narmesteLederEsyfoKafkaListenerContainerFactory")
    lateinit var esyfoFactory: ConcurrentKafkaListenerContainerFactory<String, String>

    @Autowired
    @Qualifier("aivenKafkaListenerContainerFactory")
    lateinit var eksisterendeFactory: ConcurrentKafkaListenerContainerFactory<String, String>

    @AfterEach
    fun cleanUp() {
        pdlMockWebServer.dispatcher = defaultPdlDispatcher
    }

    @Test
    fun `burde ta imot narmeste leder melding`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher { lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Supreme", etternavn = "Leader")) }

        val narmesteLederId = UUID.randomUUID()
        narmesteLederRepository.findByNarmesteLederId(narmesteLederId).shouldBeNull()

        val narmesteLederLeesah = lagNarmesteLederLeesah(narmesteLederId)
        sendNarmesteLederLeesah(narmesteLederLeesah)

        await().atMost(10, TimeUnit.SECONDS).until {
            narmesteLederRepository.findByNarmesteLederId(narmesteLederId) != null
        }

        val narmesteLeder = narmesteLederRepository.findByNarmesteLederId(narmesteLederId)
        narmesteLeder.shouldNotBeNull()
        narmesteLeder.orgnummer shouldBe narmesteLederLeesah.orgnummer
        narmesteLeder.narmesteLederNavn shouldBe "Supreme Leader"
    }

    @Test
    fun `Tombstone som tidligere ble retryet hindrer ikke ukjent status og påfølgende melding`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher { lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Supreme", etternavn = "Leader")) }

        val lederMedUkjentStatus = lagNarmesteLederLeesah(UUID.randomUUID())
        val nesteLeder = lagNarmesteLederLeesah(UUID.randomUUID())
        val meldingMedUkjentStatus = lederMedUkjentStatus.serialisertTilString().dropLast(1) + ",\"status\":\"NOE_NYTT\"}"

        listOf(
            UUID.randomUUID().toString() to null,
            lederMedUkjentStatus.narmesteLederId.toString() to meldingMedUkjentStatus,
            nesteLeder.narmesteLederId.toString() to nesteLeder.serialisertTilString(),
        ).forEach { (key, value) ->
            kafkaProducer.send(ProducerRecord(NARMESTELEDER_LEESAH_TOPIC, 0, key, value)).get()
        }

        await().atMost(10, TimeUnit.SECONDS).until {
            narmesteLederRepository.findByNarmesteLederId(nesteLeder.narmesteLederId) != null
        }
        narmesteLederRepository.findByNarmesteLederId(lederMedUkjentStatus.narmesteLederId).shouldNotBeNull()
        narmesteLederRepository.findByNarmesteLederId(nesteLeder.narmesteLederId).shouldNotBeNull()
    }

    @Test
    fun `Begge consumers oppdaterer bare sin egen tabell for samme relasjon`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher { lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Supreme", etternavn = "Leader")) }
        val container =
            kafkaListenerEndpointRegistry.listenerContainers.single {
                it.containerProperties.topics?.contains(NARMESTELEDER_ESYFO_LEESAH_TOPIC) == true
            }
        val gammel = lagNarmesteLederLeesah(UUID.randomUUID(), orgnummer = "123456789")
        val ny = gammel.copy(orgnummer = "987654321")
        try {
            container.start()
            publiserTilBeggeTabeller(gammel, ny)
            kontrollerGammelOppdateringMedAktivEsyfoConsumer(gammel, ny)
            kontrollerEsyfoOppdateringMedAktivGammelConsumer(gammel, ny)
        } finally {
            val stoppet = CountDownLatch(1)
            container.stop { stoppet.countDown() }
            check(stoppet.await(20, TimeUnit.SECONDS))
            narmesteLederEsyfoRepository.deleteByNarmesteLederId(gammel.narmesteLederId)
        }
    }

    private fun publiserTilBeggeTabeller(
        gammel: NarmesteLederLeesah,
        ny: NarmesteLederLeesah,
    ) {
        sendNarmesteLederLeesah(gammel)
        sendEsyfoMelding(ny)
        await().atMost(20, TimeUnit.SECONDS).untilAsserted {
            narmesteLederRepository.findByNarmesteLederId(gammel.narmesteLederId)?.orgnummer shouldBe gammel.orgnummer
            narmesteLederEsyfoRepository.findByNarmesteLederId(ny.narmesteLederId)?.orgnummer shouldBe ny.orgnummer
        }
    }

    private fun kontrollerGammelOppdateringMedAktivEsyfoConsumer(
        gammel: NarmesteLederLeesah,
        ny: NarmesteLederLeesah,
    ) {
        val lagretEsyfo = narmesteLederEsyfoRepository.findByNarmesteLederId(ny.narmesteLederId).shouldNotBeNull()
        val eldre = gammel.copy(timestamp = gammel.timestamp.minusDays(1), orgnummer = "888888888")
        sendNarmesteLederLeesah(eldre)
        await().atMost(20, TimeUnit.SECONDS).untilAsserted {
            narmesteLederRepository.findByNarmesteLederId(gammel.narmesteLederId)?.orgnummer shouldBe eldre.orgnummer
        }
        narmesteLederRepository.findAll().count { it.narmesteLederId == gammel.narmesteLederId } shouldBe 1
        narmesteLederEsyfoRepository.findByNarmesteLederId(ny.narmesteLederId) shouldBe lagretEsyfo
    }

    private fun kontrollerEsyfoOppdateringMedAktivGammelConsumer(
        gammel: NarmesteLederLeesah,
        ny: NarmesteLederLeesah,
    ) {
        val lagretGammel = narmesteLederRepository.findByNarmesteLederId(gammel.narmesteLederId).shouldNotBeNull()
        val oppdatert = ny.copy(orgnummer = "777777777")
        sendEsyfoMelding(oppdatert)
        await().atMost(20, TimeUnit.SECONDS).untilAsserted {
            narmesteLederEsyfoRepository.findByNarmesteLederId(ny.narmesteLederId)?.orgnummer shouldBe oppdatert.orgnummer
        }
        narmesteLederRepository.findByNarmesteLederId(gammel.narmesteLederId) shouldBe lagretGammel
    }

    private fun sendEsyfoMelding(melding: NarmesteLederLeesah) {
        kafkaProducer
            .send(
                ProducerRecord(NARMESTELEDER_ESYFO_LEESAH_TOPIC, 0, melding.narmesteLederId.toString(), melding.serialisertTilString()),
            ).get()
    }

    @Test
    fun `Consumeren for eSyfo leser historikk som batch uten å endre eksisterende consumer`() {
        kontrollerBatchKonfigurasjon()
        val container =
            kafkaListenerEndpointRegistry.listenerContainers.single {
                it.containerProperties.topics?.contains(NARMESTELEDER_ESYFO_LEESAH_TOPIC) == true
            }
        container.isRunning shouldBe false
        val melding = lagNarmesteLederLeesah(UUID.randomUUID(), orgnummer = "123456789", aktivTom = LocalDate.now())
        val siste = melding.copy(orgnummer = "987654321", timestamp = melding.timestamp.minusDays(1))
        val offset = publiserHistorikk(melding, siste)
        narmesteLederEsyfoRepository.findByNarmesteLederId(melding.narmesteLederId).shouldBeNull()
        try {
            container.start()
            await().atMost(20, TimeUnit.SECONDS).until {
                narmesteLederEsyfoRepository.findByNarmesteLederId(melding.narmesteLederId)?.orgnummer == siste.orgnummer
            }
            jdbcTemplate.queryForObject(
                "SELECT offset_sist_pafort FROM narmeste_leder_esyfo_checkpoint WHERE topic = ? AND partition = 0",
                Long::class.java,
                NARMESTELEDER_ESYFO_LEESAH_TOPIC,
            ) shouldBe offset
            narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldBeNull()
        } finally {
            container.stop()
            narmesteLederEsyfoRepository.deleteAll()
            jdbcTemplate.update("DELETE FROM narmeste_leder_esyfo_checkpoint WHERE topic = ?", NARMESTELEDER_ESYFO_LEESAH_TOPIC)
        }
    }

    private fun kontrollerBatchKonfigurasjon() {
        esyfoFactory.isBatchListener shouldBe true
        checkNotNull(esyfoFactory.consumerFactory).configurationProperties[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] shouldBe "500"
        (eksisterendeFactory.isBatchListener == true) shouldBe false
        checkNotNull(eksisterendeFactory.consumerFactory).configurationProperties[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] shouldBe "500"
    }

    private fun publiserHistorikk(
        første: NarmesteLederLeesah,
        siste: NarmesteLederLeesah,
    ): Long {
        kafkaProducer
            .send(
                ProducerRecord(NARMESTELEDER_ESYFO_LEESAH_TOPIC, 0, første.narmesteLederId.toString(), første.serialisertTilString()),
            ).get()
        return kafkaProducer
            .send(
                ProducerRecord(NARMESTELEDER_ESYFO_LEESAH_TOPIC, 0, siste.narmesteLederId.toString(), siste.serialisertTilString()),
            ).get()
            .offset()
    }

    fun sendNarmesteLederLeesah(nl: NarmesteLederLeesah) {
        kafkaProducer
            .send(
                ProducerRecord(
                    NARMESTELEDER_LEESAH_TOPIC,
                    null,
                    nl.narmesteLederId.toString(),
                    nl.serialisertTilString(),
                ),
            ).get()
    }
}
