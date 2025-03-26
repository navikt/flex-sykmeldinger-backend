package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testconfig.lesFraTopics
import no.nav.helse.flex.testconfig.subscribeHvisIkkeSubscribed
import no.nav.helse.flex.testconfig.ventPåRecords
import no.nav.helse.flex.testdata.lagStatus
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class SykmeldingStatusKafkaProducerIntegrasjonsTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var environmentToggles: EnvironmentTogglesFake

    @Autowired
    lateinit var sykmeldingStatusKafkaProducer: SykmeldingStatusKafkaProducer

    @Autowired
    lateinit var sykmeldingStatusConsumer: KafkaConsumer<String, String>

    @BeforeAll
    @AfterEach
    fun cleanUp() {
        sykmeldingStatusConsumer.lesFraTopics(SYKMELDINGSTATUS_TOPIC, ventetid = Duration.ZERO)
    }

    @Test
    fun `burde produsere sykmeldingstatus`() {
        sykmeldingStatusKafkaProducer
            .produserSykmeldingStatus(
                fnr = "fnr",
                sykmelingstatusDTO = lagStatus().event,
            ).`should be true`()

        sykmeldingStatusConsumer.subscribeHvisIkkeSubscribed(SYKMELDINGSTATUS_TOPIC)
        sykmeldingStatusConsumer.ventPåRecords(antall = 1, duration = Duration.ofSeconds(5))
    }

    @Test
    fun `burde ikke produsere meldinger til kafka i prod`() {
        environmentToggles.setEnvironment("prod")
        sykmeldingStatusKafkaProducer
            .produserSykmeldingStatus(
                fnr = "fnr",
                sykmelingstatusDTO = lagStatus().event,
            ).`should be false`()
    }
}
