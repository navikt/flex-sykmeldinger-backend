package no.nav.helse.flex

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepositoryFake
import no.nav.helse.flex.narmesteleder.NARMESTELEDER_LEESAH_TOPIC
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.narmesteleder.NarmesteLederRepositoryFake
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import no.nav.helse.flex.sykmelding.SykmeldingRepositoryFake
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

private class PostgreSQLContainer14V2 : PostgreSQLContainer<PostgreSQLContainer14V2>("postgres:14-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(
    classes = [Application::class, FakesTestOppsett.TestConfig::class],
    properties = [
        "spring.main.allow-bean-definition-overriding=true",
        "spring.data.jdbc.repositories.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration",
        // "spring.kafka.listener.auto-startup=false",
        "spring.flyway.enabled=false",
    ],
)
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
abstract class FakesTestOppsett {
    @TestConfiguration
    class TestConfig {
        @Bean
        fun sykmeldingRepository(): ISykmeldingRepository = SykmeldingRepositoryFake()

        @Bean
        fun arbeidsforholdRepository(): ArbeidsforholdRepository = ArbeidsforholdRepositoryFake()

        @Bean
        fun narmesteLederRepository(): NarmesteLederRepository = NarmesteLederRepositoryFake()
    }

    @Autowired
    lateinit var kafkaConsumer: KafkaConsumer<String, String>

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var server: MockOAuth2Server

    @Autowired
    lateinit var kafkaProducer: Producer<String, String>

    @Autowired
    lateinit var narmesteLederRepository: NarmesteLederRepository

    @Autowired
    lateinit var arbeidsforholdRepository: ArbeidsforholdRepository

    @Autowired
    lateinit var sykmeldingRepository: ISykmeldingRepository

    @Autowired
    lateinit var kafkaListenerRegistry: KafkaListenerEndpointRegistry

    companion object {
        init {

            KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1")).apply {
                start()
                System.setProperty("KAFKA_BROKERS", bootstrapServers)
            }

            startMockWebServere()
        }
    }

    @AfterAll
    fun `Vi resetter databasen`() {
        slettDatabase()
    }

    fun slettDatabase() {
        narmesteLederRepository.deleteAll()
        arbeidsforholdRepository.deleteAll()
        sykmeldingRepository.deleteAll()
    }

    fun ventPaConsumers() {
        // Burde brukes dersom consumere har offset=latest
        kafkaListenerRegistry.listenerContainers.forEach { container ->
            ContainerTestUtils.waitForAssignment(container, 1)
        }
    }

    fun sendNarmesteLederLeesah(nl: NarmesteLederLeesah) {
        kafkaProducer
            .send(
                ProducerRecord(
                    NARMESTELEDER_LEESAH_TOPIC,
                    null,
                    nl.narmesteLederId.toString(),
                    nl.serialisertTilString(),
                ),
            ).get()
    }
}
