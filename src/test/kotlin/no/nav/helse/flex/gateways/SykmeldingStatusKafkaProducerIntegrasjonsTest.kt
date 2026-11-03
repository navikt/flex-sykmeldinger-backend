package no.nav.helse.flex.gateways

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeSingle
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.lesFraTopics
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaDTO
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaMessageDTO
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

class SykmeldingStatusKafkaProducerIntegrasjonsTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusKafkaProducer: SykmeldingStatusKafkaProducerImpl

    @Autowired
    lateinit var sykmeldingStatusConsumer: KafkaConsumer<String, String>

    @BeforeAll
    fun beforeAll() {
        // Workaround, andre integrasjonstester rydder ikke opp etter seg
        sykmeldingStatusConsumer.lesFraTopics(SYKMELDINGSTATUS_TOPIC, ventetid = Duration.ofSeconds(1))
    }

    @AfterEach
    fun afterEach() {
        sykmeldingStatusConsumer.lesFraTopics(SYKMELDINGSTATUS_TOPIC, ventetid = Duration.ZERO)
    }

    @Test
    fun `burde produsere sykmeldingstatus`() {
        sykmeldingStatusConsumer.subscribe(listOf(SYKMELDINGSTATUS_TOPIC))

        sykmeldingStatusKafkaProducer
            .produserSykmeldingStatus(
                sykmeldingStatusKafkaMessageDTO =
                    lagSykmeldingStatusKafkaMessageDTO(
                        event =
                            lagSykmeldingStatusKafkaDTO(
                                sykmeldingId = "1",
                            ),
                    ),
            ).shouldBeTrue()

        val consumerRecords =
            sykmeldingStatusConsumer
                .poll(Duration.ofSeconds(1))

        consumerRecords
            .shouldBeSingle()
            .run {
                key() shouldBe "1"
                value().shouldNotBeNull()
            }
    }
}
