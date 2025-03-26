package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testconfig.hentProduserteRecords
import no.nav.helse.flex.testdata.lagStatus
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
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

    @AfterEach
    fun cleanUp() {
        sykmeldingStatusConsumer.hentProduserteRecords(Duration.ZERO)
    }

    @Test
    fun `burde produsere sykmeldingstatus`() {
        sykmeldingStatusKafkaProducer
            .produserSykmeldingStatus(
                fnr = "fnr",
                sykmelingstatusDTO = lagStatus().event,
            ).`should be true`()

        sykmeldingStatusConsumer.subscribe(listOf("teamsykmelding.sykmeldingstatus-leesah"))
        sykmeldingStatusConsumer.poll(Duration.ofSeconds(5)).count() `should be equal to` 1
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
