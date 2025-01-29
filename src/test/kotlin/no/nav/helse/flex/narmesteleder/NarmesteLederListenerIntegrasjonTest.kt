package no.nav.helse.flex.narmesteleder

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import no.nav.helse.flex.notFoundDispatcher
import no.nav.helse.flex.pdl.lagGetPersonResponseData
import no.nav.helse.flex.pdl.lagGraphQlResponse
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.simpleDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.TimeUnit

class NarmesteLederListenerIntegrasjonTest : FellesTestOppsett() {
    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

    @AfterEach
    fun cleanUp() {
        pdlMockWebServer.dispatcher = notFoundDispatcher
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
        narmesteLeder.orgnummer `should be equal to` narmesteLederLeesah.orgnummer
        narmesteLeder.narmesteLederNavn `should be equal to` "Supreme Leader"
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
