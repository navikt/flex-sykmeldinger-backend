package no.nav.helse.flex.testconfig

import no.nav.helse.flex.Application
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

private class ValkeyContainer : GenericContainer<ValkeyContainer>("bitnami/valkey:8.0.2")

private class PostgreSQLContainer14 : PostgreSQLContainer<PostgreSQLContainer14>("postgres:14-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class, KafkaTestConfig::class, MockWebServereConfig::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
abstract class IntegrasjonTestOppsett {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var kafkaProducer: Producer<String, String>

    @Autowired
    lateinit var narmesteLederRepository: NarmesteLederRepository

    @Autowired
    lateinit var arbeidsforholdRepository: ArbeidsforholdRepository

    @Autowired
    lateinit var sykmeldingRepository: SykmeldingRepository

    @Autowired
    lateinit var kafkaListenerRegistry: KafkaListenerEndpointRegistry

    companion object {
        init {

            KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1")).apply {
                start()
                System.setProperty("KAFKA_BROKERS", bootstrapServers)
            }

            PostgreSQLContainer14().apply {
                withCommand("postgres", "-c", "wal_level=logical")
                start()
                System.setProperty("spring.datasource.url", "$jdbcUrl&reWriteBatchedInserts=true")
                System.setProperty("spring.datasource.username", username)
                System.setProperty("spring.datasource.password", password)
            }

            ValkeyContainer().apply {
                withEnv("ALLOW_EMPTY_PASSWORD", "yes")
                withExposedPorts(6379)
                start()

                System.setProperty("VALKEY_URI_SESSIONS", "rediss://$host:$firstMappedPort")
                System.setProperty("VALKEY_USERNAME_SESSIONS", "default")
                System.setProperty("VALKEY_PASSWORD_SESSIONS", "")
            }
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
}
