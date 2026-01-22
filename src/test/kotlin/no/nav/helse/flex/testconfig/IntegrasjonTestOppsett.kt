package no.nav.helse.flex.testconfig

import no.nav.helse.flex.Application
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.sykmelding.SykmeldingRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.web.servlet.MockMvc

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(
    properties = [
        "spring.main.allow-bean-definition-overriding=true",
    ],
    classes = [
        Application::class, KafkaTestConfig::class, MockWebServereConfig::class, IntegrasjonsTestConfig::class,
    ],
)
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
    lateinit var kafkaListenerEndpointRegistry: KafkaListenerEndpointRegistry

    init {
        TestcontainersOppsett.initIfNotRunning()
    }

    @BeforeAll
    fun beforeAllFelles() {
        ventPaConsumers()
    }

    @AfterAll
    fun afterAllFelles() {
        slettDatabase()
    }

    fun slettDatabase() {
        narmesteLederRepository.deleteAll()
        arbeidsforholdRepository.deleteAll()
        sykmeldingRepository.deleteAll()
    }

    private fun ventPaConsumers() {
        kafkaListenerEndpointRegistry.listenerContainers.forEach { container ->
            ContainerTestUtils.waitForAssignment(container, 1)
        }
    }
}
