package no.nav.helse.flex.listeners

import no.nav.helse.flex.sykmelding.EksternSykmeldingMelding
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagValidation
import no.nav.helse.flex.testdatagenerator.TEST_SYKMELDING_TOPIC
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Duration

class TestSykmeldingListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @Test
    fun `burde lagre sykmelding fra kafka`() {
        val topic = TEST_SYKMELDING_TOPIC
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
        val topic = TEST_SYKMELDING_TOPIC
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
