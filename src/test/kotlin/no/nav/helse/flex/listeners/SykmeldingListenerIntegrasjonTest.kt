package no.nav.helse.flex.listeners

import no.nav.helse.flex.sykmelding.domain.SykmeldingKafkaRecord
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagValidation
import no.nav.helse.flex.testdatagenerator.TEST_SYKMELDING_TOPIC
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.`should be empty`
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class SykmeldingListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var environmentToggles: EnvironmentTogglesFake

    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @ParameterizedTest
    @ValueSource(strings = [TEST_SYKMELDING_TOPIC, SYKMELDING_TOPIC])
    fun `burde lagre sykmelding fra kafka`(topic: String) {
        environmentToggles.setEnvironment("dev")

        val kafkaMelding =
            SykmeldingKafkaRecord(
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
    fun `burde ikke lagre sykmelding i prod`() {
        environmentToggles.setEnvironment("prod")

        val kafkaMelding =
            SykmeldingKafkaRecord(
                sykmelding = lagSykmeldingGrunnlag(id = "1"),
                validation = lagValidation(),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDING_TOPIC,
                    null,
                    "1",
                    kafkaMelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            sykmeldingRepository.findAll().`should be empty`()
        }
    }
}
