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
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.TimeUnit

class NarmesteLederKafkaListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

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
    fun `tombstone som tidligere ble retryet hindrer ikke ukjent status og påfølgende melding`() {
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
    fun `gammel consumer forblir på gammel tabell mens ny consumer er avslått`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher { lagGraphQlResponse(lagGetPersonResponseData(fornavn = "Supreme", etternavn = "Leader")) }

        val melding = lagNarmesteLederLeesah(UUID.randomUUID())
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
