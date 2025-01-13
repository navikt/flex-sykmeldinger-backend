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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class SykmeldingLagrerIntegrasjonTest : FellesTestOppsett() {
    @AfterEach
    fun tearDown() {
        slettDatabase()
        slettKafka()
    }

    @ParameterizedTest
    @ValueSource(strings = [SYKMELDING_TOPIC, TEST_SYKMELDING_TOPIC])
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
                kafkaMelding.sykmelding.id,
                kafkaMelding.serialisertTilString(),
            ),
        ).get()

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            sykemeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
        }
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

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            sykemeldingRepository.findAll().size `should be equal to` 1
        }
    }
}
