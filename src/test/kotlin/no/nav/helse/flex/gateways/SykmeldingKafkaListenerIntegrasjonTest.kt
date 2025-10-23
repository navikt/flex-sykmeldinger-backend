package no.nav.helse.flex.gateways

import no.nav.helse.flex.sykmelding.EksternSykmeldingMelding
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class SykmeldingKafkaListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    private lateinit var sykmeldingListener: SykmeldingListener

    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @Test
    fun `burde lagre NORSK sykmelding fra kafka`() {
        val eksternSykmeldingMelding =
            EksternSykmeldingMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDING_TOPIC,
                    null,
                    "1",
                    eksternSykmeldingMelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).until {
            sykmeldingRepository.findBySykmeldingId("1") != null
        }

        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
    }

    @Test
    fun `burde lagre DIGITAL sykmelding fra kafka`() {
        val eksternSykmeldingMelding =
            lagEksternSykmeldingMelding(
                sykmelding = lagDigitalSykmeldingGrunnlag(),
                validation = lagValidation(),
            )

        sykmeldingListener.listen(
            cr =
                ConsumerRecord(
                    SYKMELDING_TOPIC,
                    0,
                    0,
                    "1",
                    eksternSykmeldingMelding.serialisertTilString(),
                ),
            acknowledgment = { },
        )

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
}
