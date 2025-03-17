package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.EnvironmentConfig
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import org.amshove.kluent.`should be equal to`
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import java.time.Duration
import java.time.OffsetDateTime

@Import(EnvironmentConfig::class)
class SykmeldingStatusKafkaProducerIntegrasjonsTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var environmentToggles: EnvironmentTogglesFake

    @Autowired
    lateinit var sykmeldingStatusKafkaProducer: SykmeldingStatusKafkaProducer

    @Autowired
    lateinit var sykmeldingStatusConsumer: KafkaConsumer<String, String>

    @Test
    fun `burde produsere sykmeldingstatus`() {
        sykmeldingStatusKafkaProducer.produserSykmeldingStatus(
            fnr = "fnr",
            sykmelingstatusDTO =
                SykmeldingStatusKafkaDTO(
                    sykmeldingId = "1",
                    timestamp = OffsetDateTime.parse("2021-09-01T00:00:00Z"),
                    statusEvent = HendelseStatus.SENDT_TIL_NAV.name,
                ),
        )

        sykmeldingStatusConsumer.subscribe(listOf("teamsykmelding.sykmeldingstatus-leesah"))
        sykmeldingStatusConsumer.poll(Duration.ofSeconds(10)).count() `should be equal to` 1
    }

    @Test
    fun `burde ikke produsere meldinger til kafka i prod`() {
        environmentToggles.setEnvironment("prod")
        sykmeldingStatusKafkaProducer.produserSykmeldingStatus(
            fnr = "fnr",
            sykmelingstatusDTO =
                SykmeldingStatusKafkaDTO(
                    sykmeldingId = "1",
                    timestamp = OffsetDateTime.parse("2021-09-01T00:00:00Z"),
                    statusEvent = HendelseStatus.SENDT_TIL_NAV.name,
                ),
        )

        sykmeldingStatusConsumer.subscribe(listOf("teamsykmelding.sykmeldingstatus-leesah"))
        sykmeldingStatusConsumer.poll(Duration.ofMillis(100)).count() `should be equal to` 0
    }
}
