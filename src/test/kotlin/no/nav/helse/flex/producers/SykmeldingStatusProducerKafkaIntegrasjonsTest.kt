package no.nav.helse.flex.producers

import no.nav.helse.flex.producers.SykmeldingStatusProducerKafka
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testconfig.lesFraTopics
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.tsmsykmeldingstatus.SYKMELDINGSTATUS_TOPIC
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class SykmeldingStatusProducerKafkaIntegrasjonsTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var environmentToggles: EnvironmentTogglesFake

    @Autowired
    lateinit var sykmeldingStatusProducerKafka: SykmeldingStatusProducerKafka

    @Autowired
    lateinit var sykmeldingStatusConsumer: KafkaConsumer<String, String>

    @BeforeAll
    @AfterEach
    fun cleanUp() {
        sykmeldingStatusConsumer.lesFraTopics(SYKMELDINGSTATUS_TOPIC, ventetid = Duration.ZERO)
    }

    @Test
    fun `burde produsere sykmeldingstatus`() {
        sykmeldingStatusConsumer.subscribe(listOf("teamsykmelding.sykmeldingstatus-leesah"))
        val antallForProdusert = sykmeldingStatusConsumer.poll(Duration.ofSeconds(1)).count()

        sykmeldingStatusProducerKafka
            .produserSykmeldingStatus(
                sykmeldingStatusKafkaMessageDTO = lagSykmeldingStatusKafkaMessageDTO(),
            ).`should be true`()

        antallForProdusert +
            sykmeldingStatusConsumer
                .poll(Duration.ofSeconds(1))
                .count() `should be equal to` antallForProdusert + 1
    }

    @Test
    fun `burde ikke produsere meldinger til kafka i prod`() {
        environmentToggles.setEnvironment("prod")
        sykmeldingStatusProducerKafka
            .produserSykmeldingStatus(
                sykmeldingStatusKafkaMessageDTO = lagSykmeldingStatusKafkaMessageDTO(),
            ).`should be false`()
    }
}
