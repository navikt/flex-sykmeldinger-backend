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
    fun `Meldinger fra team sykmelding lagres i eksisterende tabell mens eSyfo-consumeren er avslått`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher { lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Supreme", etternavn = "Leader")) }

        val melding = lagNarmesteLederLeesah(UUID.randomUUID(), orgnummer = "123456789")
        val eldreMelding = melding.copy(timestamp = melding.timestamp.minusDays(1), orgnummer = "888888")
        val markor = lagNarmesteLederLeesah(UUID.randomUUID())

        kafkaProducer
            .send(
                ProducerRecord(NARMESTELEDER_ESYFO_LEESAH_TOPIC, 0, melding.narmesteLederId.toString(), melding.serialisertTilString()),
            ).get()
        narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId).shouldBeNull()

        listOf(melding, eldreMelding, markor).forEach {
            kafkaProducer
                .send(
                    ProducerRecord(NARMESTELEDER_LEESAH_TOPIC, 0, it.narmesteLederId.toString(), it.serialisertTilString()),
                ).get()
        }
        await().atMost(10, TimeUnit.SECONDS).until {
            narmesteLederRepository.findByNarmesteLederId(markor.narmesteLederId) != null
        }

        narmesteLederRepository.findAll().count { it.narmesteLederId == melding.narmesteLederId } shouldBe 1
        narmesteLederRepository.findByNarmesteLederId(melding.narmesteLederId)?.orgnummer shouldBe eldreMelding.orgnummer
        narmesteLederEsyfoRepository.findByNarmesteLederId(melding.narmesteLederId).shouldBeNull()
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
