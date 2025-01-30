package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.kafka.SYKMELDING_TOPIC
import no.nav.helse.flex.kafka.SykmeldingListener
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.sykmelding.domain.StatusEvent
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import no.nav.helse.flex.testdata.TEST_SYKMELDING_TOPIC
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.support.Acknowledgment
import java.time.Duration

private val NOOP_ACK = Acknowledgment {}

class GammelSykmeldingLagrerIntegrasjonTest : FellesTestOppsett() {
    @Autowired
    lateinit var sykmeldingListener: SykmeldingListener

    @AfterEach
    fun tearDown() {
        slettDatabase()
    }

    @ParameterizedTest
    @ValueSource(strings = [TEST_SYKMELDING_TOPIC, SYKMELDING_TOPIC])
    fun `burde lagre sykmelding fra kafka`(topic: String) {
        val kafkaMelding =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        super.ventPaConsumers()

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
    fun `burde deduplisere sykmeldinger`() {
        val kafkaMelding =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        repeat(2) { i ->
            sykmeldingListener.listen(
                ConsumerRecord(
                    "",
                    -1,
                    i.toLong(),
                    kafkaMelding.sykmelding.id,
                    kafkaMelding.serialisertTilString(),
                ),
                NOOP_ACK,
            )
        }

        sykmeldingRepository.findAll().size `should be equal to` 1
    }

    @Test
    fun `burde sette status til ny`() {
        val kafkaMelding =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        sykmeldingListener.listen(
            ConsumerRecord(
                "",
                -1,
                1L,
                kafkaMelding.sykmelding.id,
                kafkaMelding.serialisertTilString(),
            ),
            NOOP_ACK,
        )

        val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
        sykmelding.shouldNotBeNull()
        sykmelding.statuser.size `should be equal to` 1
        val status = sykmelding.statuser[0]
        status.status `should be equal to` StatusEvent.APEN
    }
}
