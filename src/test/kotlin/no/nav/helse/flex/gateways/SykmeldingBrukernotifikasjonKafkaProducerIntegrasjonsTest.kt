package no.nav.helse.flex.gateways

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.lesFraTopics
import no.nav.helse.flex.utils.objectMapper
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSingleItem
import org.amshove.kluent.shouldNotBeNull
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class SykmeldingBrukernotifikasjonKafkaProducerIntegrasjonsTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var sykmeldingBrukernotifikasjonProducer: SykmeldingBrukernotifikasjonKafkaProducer

    @Autowired
    lateinit var kafkaConsumer: KafkaConsumer<String, String>

    @Test
    fun `burde publisere notifikasjon til kafka`() {
        sykmeldingBrukernotifikasjonProducer.produserSykmeldingBrukernotifikasjon(
            sykmeldingNotifikasjon =
                SykmeldingNotifikasjon(
                    sykmeldingId = "1",
                    status = SykmeldingNotifikasjonStatus.INVALID,
                    mottattDato = LocalDateTime.parse("2024-06-01T12:00:00"),
                    fnr = "fnr",
                ),
        )
        await().atMost(2, TimeUnit.SECONDS).untilAsserted {
            val eventer = kafkaConsumer.lesFraTopics(SYKMELDING_BRUKERNOTIFIKASJON_TOPIC, ventetid = Duration.ofSeconds(1))
            eventer.shouldHaveSingleItem()
        }
    }

    @Test
    fun `publisert notifikasjon burde ha riktig data`() {
        sykmeldingBrukernotifikasjonProducer.produserSykmeldingBrukernotifikasjon(
            sykmeldingNotifikasjon =
                SykmeldingNotifikasjon(
                    sykmeldingId = "1",
                    status = SykmeldingNotifikasjonStatus.INVALID,
                    mottattDato = LocalDateTime.parse("2024-06-01T12:00:00"),
                    fnr = "fnr",
                ),
        )
        var record: ConsumerRecord<String, String>? = null
        await().atMost(2, TimeUnit.SECONDS).until {
            val eventer = kafkaConsumer.lesFraTopics(SYKMELDING_BRUKERNOTIFIKASJON_TOPIC, ventetid = Duration.ofSeconds(1))
            record = eventer.firstOrNull()
            record != null
        }

        record
            .shouldNotBeNull()
            .let { objectMapper.readValue<SykmeldingNotifikasjon>(it.value()) }
            .run {
                sykmeldingId shouldBeEqualTo "1"
                status shouldBeEqualTo SykmeldingNotifikasjonStatus.INVALID
                mottattDato shouldBeEqualTo LocalDateTime.parse("2024-06-01T12:00:00")
                fnr shouldBeEqualTo "fnr"
            }
    }
}
