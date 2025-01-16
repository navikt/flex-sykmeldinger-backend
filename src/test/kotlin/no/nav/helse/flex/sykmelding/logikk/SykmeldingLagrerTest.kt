package no.nav.helse.flex.sykmelding.logikk

import no.nav.helse.flex.Application
import no.nav.helse.flex.kafka.SykmeldingListener
import no.nav.helse.flex.serialisertTilString
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.domain.SykmeldingMedBehandlingsutfallMelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingRepositoryFake
import no.nav.helse.flex.sykmelding.domain.lagSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.lagValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.amshove.kluent.`should not be null`
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.Acknowledgment

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableMockOAuth2Server
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
@SpringBootTest(
    properties = [
        "spring.kafka.listener.auto-startup=false",
        "spring.flyway.enabled=false",
        "spring.datasource.url=",
        "AAREG_URL=url",
        "EREG_URL=url",
    ],
    classes = [Application::class],
)
class SykmeldingLagrerTest {
    @Configuration
    class SykmeldingLagrerConfiguration {
        @Bean
        fun sykmeldingRepository(): ISykmeldingRepository = SykmeldingRepositoryFake()
    }

    private val noAcknoledgment = Acknowledgment {}

    @Autowired
    private lateinit var sykmeldingRepository: ISykmeldingRepository

    @Autowired
    lateinit var sykmeldingListener: SykmeldingListener

    @Test
    fun `burde lagre sykmelding fra kafka`() {
        val sykmeldingMedBehandlingsutfall =
            SykmeldingMedBehandlingsutfallMelding(
                sykmelding = lagSykmeldingGrunnlag(),
                validation = lagValidation(),
            )
        sykmeldingListener.listen(
            cr = ConsumerRecord("TOPIC", 1, 1, "sykmelding", sykmeldingMedBehandlingsutfall.serialisertTilString()),
            acknowledgment = noAcknoledgment,
        )
        val lagretSykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingMedBehandlingsutfall.sykmelding.id)
        lagretSykmelding.`should not be null`()
    }
}
