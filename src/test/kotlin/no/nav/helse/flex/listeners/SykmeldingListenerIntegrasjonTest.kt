package no.nav.helse.flex.listeners

import no.nav.helse.flex.sykmelding.domain.SykmeldingKafkaRecord
import no.nav.helse.flex.sykmelding.domain.lagMeldingsinformasjonEgenmeldt
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdatagenerator.TEST_SYKMELDING_TOPIC
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Duration

class SykmeldingListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @AfterEach
    fun tearDown() {
        slettDatabase()
    }

    @ParameterizedTest
    @ValueSource(strings = [TEST_SYKMELDING_TOPIC, SYKMELDING_TOPIC])
    fun `burde lagre sykmelding fra kafka`(topic: String) {
        val kafkaMelding =
            SykmeldingKafkaRecord(
                metadata = lagMeldingsinformasjonEgenmeldt(),
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
}
