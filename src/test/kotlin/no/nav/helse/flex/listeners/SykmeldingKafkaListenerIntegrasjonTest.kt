package no.nav.helse.flex.listeners

import no.nav.helse.flex.sykmelding.EksternSykmeldingMelding
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagValidation
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.*
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class SykmeldingKafkaListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    private lateinit var environmentToggles: EnvironmentTogglesFake

    @Autowired
    private lateinit var sykmeldingListener: SykmeldingListener

    @BeforeEach
    fun beforeEach() {
        environmentToggles.setEnvironment("prod")
    }

    @AfterEach
    fun afterEach() {
        slettDatabase()
        environmentToggles.reset()
    }

    @Test
    fun `burde lagre sykmelding fra kafka`() {
        val topic = SYKMELDING_TOPIC
        val kafkaMelding =
            EksternSykmeldingMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    topic,
                    null,
                    "1",
                    kafkaMelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).until {
            sykmeldingRepository.findBySykmeldingId("1") != null
        }

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
    }

    @Test
    fun `burde tombstone sykmelding fra kafka`() {
        val topic = SYKMELDING_TOPIC
        sykmeldingRepository.save(
            lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")),
        )

        val key = "1"
        val value = null
        kafkaProducer
            .send(ProducerRecord(topic, null, key, value))
            .get()

        await().atMost(Duration.ofSeconds(2)).until {
            sykmeldingRepository.findBySykmeldingId("1") == null
        }

        sykmeldingRepository.findBySykmeldingId("1").shouldBeNull()
    }

    @Test
    fun `sykmelding type DIGITAL burde ignorere i dev miljø`() {
        val sykmeldingJson =
            """
            {
              "sykmelding": {
                "id": "1",
                "type": "DIGITAL"
              }
            }
            """.trimIndent()
        environmentToggles.setEnvironment("dev")

        sykmeldingListener.listen(
            cr =
                ConsumerRecord(
                    SYKMELDING_TOPIC,
                    0,
                    0,
                    "1",
                    sykmeldingJson,
                ),
            acknowledgment = { },
        )

        sykmeldingRepository.findBySykmeldingId("1").shouldBeNull()
    }

    @Test
    fun `sykmelding type DIGITAL burde feile i prod miljø`() {
        val sykmeldingJson =
            """
            {
              "sykmelding": {
                "id": "1",
                "type": "DIGITAL"
              }
            }
            """.trimIndent()
        environmentToggles.setEnvironment("prod")

        invoking {
            sykmeldingListener.listen(
                cr =
                    ConsumerRecord(
                        SYKMELDING_TOPIC,
                        0,
                        0,
                        "1",
                        sykmeldingJson,
                    ),
                acknowledgment = { },
            )
        }.shouldThrow(Exception::class)
    }
}
