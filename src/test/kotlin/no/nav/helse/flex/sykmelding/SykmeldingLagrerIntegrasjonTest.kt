package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.kafka.SYKMELDING_TOPIC
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import no.nav.helse.flex.testdata.TEST_SYKMELDING_TOPIC
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.Instant
import java.util.function.Supplier

class SykmeldingLagrerIntegrasjonTest : FellesTestOppsett() {
    @Configuration
    class TetsConfiguration {
        @Bean
        fun nowFactory(): Supplier<Instant> {
            return Supplier { Instant.parse("2020-01-01T00:00:00.000Z") }
        }
    }

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

    @ParameterizedTest
    @ValueSource(strings = [SYKMELDING_TOPIC, TEST_SYKMELDING_TOPIC])
    fun `burde ikke lagre sykmelding duplikat`(topic: String) {
        val kafkaMelding =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        repeat(2) {
            kafkaProducer.send(
                ProducerRecord(
                    topic,
                    null,
                    kafkaMelding.sykmelding.id,
                    kafkaMelding.serialisertTilString(),
                ),
            ).get()
        }
        // for å kontrollere at de forrige to ble lest før vi asserter
        kafkaProducer.send(
            ProducerRecord(
                topic,
                null,
                kafkaMelding.sykmelding.id,
                SykmeldingMedBehandlingsutfallMelding(
                    sykmelding = lagSykmeldingGrunnlag(id = "2"),
                    validation = lagValidation(),
                ).serialisertTilString(),
            ),
        ).get()

        await()
            .atMost(Duration.ofSeconds(10)).untilAsserted {
                sykemeldingRepository.findAll().size `should be equal to` 2
            }
    }
}
