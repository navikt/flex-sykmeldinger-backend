package no.nav.helse.flex

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepositoryFake
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.narmesteleder.NarmesteLederRepositoryFake
import no.nav.helse.flex.sykmelding.SykmeldingRepositoryFake
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.web.servlet.MockMvc

const val IGNORED_KAFKA_BROKERS = "localhost:1"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(
    classes = [Application::class, FakesTestOppsett.TestConfig::class],
    properties = [
        "spring.main.allow-bean-definition-overriding=true",
        "spring.data.jdbc.repositories.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.flyway.enabled=false",
        "spring.kafka.listener.auto-startup=false",
        "KAFKA_BROKERS=$IGNORED_KAFKA_BROKERS",
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
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var narmesteLederRepository: NarmesteLederRepository

    @Autowired
    lateinit var arbeidsforholdRepository: ArbeidsforholdRepository

    @Autowired
    lateinit var sykmeldingRepository: ISykmeldingRepository

    companion object {
        init {
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
}
