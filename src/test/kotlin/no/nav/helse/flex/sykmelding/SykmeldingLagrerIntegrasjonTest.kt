package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.kafka.SYKMELDING_TOPIC
import no.nav.helse.flex.kafka.SykmeldingListener
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import no.nav.helse.flex.testdata.TEST_SYKMELDING_TOPIC
import no.nav.helse.flex.testdata.TestSykmeldingListener
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.Acknowledgment
import java.time.Duration
import java.time.Instant
import java.util.function.Supplier

private val NOOP_ACK = Acknowledgment {}

class SykmeldingLagrerIntegrasjonTest : FellesTestOppsett() {
    @Configuration
    class TetsConfiguration {
        @Bean
        fun nowFactory(): Supplier<Instant> {
            return Supplier { Instant.parse("2020-01-01T00:00:00.000Z") }
        }
    }

    @Autowired
    lateinit var sykmeldingListener: SykmeldingListener

    @Autowired
    lateinit var testSykmeldingListener: TestSykmeldingListener

    @BeforeAll
    fun setup() {
        super.ventPaConsumers()
        println("Ferdig venta på consumers")
    }

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

        kafkaProducer.send(
            ProducerRecord(
                topic,
                null,
                "1",
                kafkaMelding.serialisertTilString(),
            ),
        ).get()

        await().atMost(Duration.ofSeconds(2)).until {
            sykemeldingRepository.findBySykmeldingId("1") != null
        }

        sykemeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
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

        sykemeldingRepository.findAll().size `should be equal to` 1
    }

    @Test
    fun `burde deduplisere testdata sykmeldinger`() {
        val kafkaMelding =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        repeat(2) { i ->
            testSykmeldingListener.listen(
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

        sykemeldingRepository.findAll().size `should be equal to` 1
    }
}
