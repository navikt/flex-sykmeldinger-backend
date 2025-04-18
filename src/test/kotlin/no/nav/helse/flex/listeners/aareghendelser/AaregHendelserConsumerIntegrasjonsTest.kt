package no.nav.helse.flex.listeners.aareghendelser

import no.nav.helse.flex.arbeidsforhold.innhenting.lagArbeidsforholdOversiktResponse
import no.nav.helse.flex.clients.EKSEMPEL_RESPONSE_FRA_EREG
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.defaultAaregDispatcher
import no.nav.helse.flex.testconfig.defaultEregDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import no.nav.helse.flex.utils.serialisertTilString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import java.time.Duration

class AaregHendelserConsumerIntegrasjonsTest : IntegrasjonTestOppsett() {
    @Value("\${AAREG_HENDELSE_TOPIC}")
    lateinit var aaregTopic: String

    @Autowired
    private lateinit var aaregMockWebServer: MockWebServer

    @Autowired
    private lateinit var eregMockWebServer: MockWebServer

    @BeforeAll
    fun beforeAll() {
        aaregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(
                        lagArbeidsforholdOversiktResponse().serialisertTilString(),
                    ).addHeader("Content-Type", "application/json")
            }

        eregMockWebServer.dispatcher =
            simpleDispatcher {
                MockResponse()
                    .setBody(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
                    .addHeader("Content-Type", "application/json")
            }
    }

    @AfterAll
    fun afterAll() {
        aaregMockWebServer.dispatcher = defaultAaregDispatcher
        eregMockWebServer.dispatcher = defaultEregDispatcher
    }

    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @Test
    fun `burde lese arbeidsforhold hendelse, og lagre endret arbeidsforhold fra aareg + ereg`() {
        val record: ProducerRecord<String, String> =
            ProducerRecord(
                aaregTopic,
                null,
                "key",
                lagArbeidsforholdHendelse(fnr = "_").serialisertTilString(),
            )
        kafkaProducer.send(record).get()

        await().atMost(Duration.ofSeconds(20)).until {
            arbeidsforholdRepository.findAll().count() >= 1
        }
    }
}
