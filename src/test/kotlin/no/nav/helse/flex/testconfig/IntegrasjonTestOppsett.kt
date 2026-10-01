package no.nav.helse.flex.testconfig

import io.getunleash.FakeUnleash
import no.nav.helse.flex.Application
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.optin.OptInDbRepository
import no.nav.helse.flex.outbox.OutboxDbRepository
import no.nav.helse.flex.sykmelding.SykmeldingRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.micrometer.metrics.test.autoconfigure.AutoConfigureMetrics
import org.springframework.boot.micrometer.tracing.test.autoconfigure.AutoConfigureTracing
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.web.servlet.MockMvc

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMetrics
@AutoConfigureTracing
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
    lateinit var outboxDbRepository: OutboxDbRepository

    @Autowired
    lateinit var optInDbRepository: OptInDbRepository

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

    @Autowired
    lateinit var fakeUnleash: FakeUnleash

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
        fakeUnleash.resetAll()
    }

    fun slettDatabase() {
        narmesteLederRepository.deleteAll()
        arbeidsforholdRepository.deleteAll()
        sykmeldingRepository.deleteAll()
        outboxDbRepository.deleteAll()
        optInDbRepository.deleteAll()
    }

    private fun ventPaConsumers() {
        kafkaListenerEndpointRegistry.listenerContainers.forEach { container ->
            ContainerTestUtils.waitForAssignment(container, 1)
        }
    }
}
