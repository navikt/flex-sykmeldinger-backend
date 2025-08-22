package no.nav.helse.flex.listeners

import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.fakes.EnvironmentTogglesFake
import no.nav.helse.flex.testconfig.fakes.NowFactoryFake
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.SYKMELDINGSTATUS_TOPIC
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.shouldBeEqualTo
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

class SykmeldingStatusListenerIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    private lateinit var nowFactoryFake: NowFactoryFake

    @Autowired
    private lateinit var environmentToggles: EnvironmentTogglesFake

    @AfterEach
    fun tearDown() {
        environmentToggles.reset()
        nowFactoryFake.reset()
        slettDatabase()
    }

    @Test
    fun `burde lagre hendelse fra kafka`() {
        environmentToggles.setEnvironment("prod")

        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val kafkamelding =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1", fnr = "fnr"),
                event = lagSykmeldingStatusKafkaDTO(statusEvent = "SENDT"),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDINGSTATUS_TOPIC,
                    null,
                    "1",
                    kafkamelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            sykmeldingRepository.findBySykmeldingId("1")?.sisteHendelse()?.status shouldBeEqualTo HendelseStatus.SENDT_TIL_ARBEIDSGIVER
        }
    }

    @Test
    fun `burde ikke lagre hendelse fra kafka i dev som er eldre enn 2 måneder`() {
        environmentToggles.setEnvironment("dev")
        nowFactoryFake.setNow(Instant.parse("2025-01-01T00:00:00.00Z"))
        val toManederSiden = nowFactoryFake.get().minus(61, java.time.temporal.ChronoUnit.DAYS)

        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val kafkamelding =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1", fnr = "fnr"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SENDT",
                        timestamp = OffsetDateTime.ofInstant(toManederSiden, ZoneId.of("UTC")),
                    ),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDINGSTATUS_TOPIC,
                    null,
                    "1",
                    kafkamelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            sykmeldingRepository.findBySykmeldingId("1")?.sisteHendelse()?.status shouldBeEqualTo HendelseStatus.APEN
        }
    }

    @Test
    fun `burde lagre hendelse fra kafka i dev som er nyere enn 2 måneder`() {
        environmentToggles.setEnvironment("dev")
        nowFactoryFake.setNow(Instant.parse("2025-01-01T00:00:00.00Z"))
        val toManederSiden = nowFactoryFake.get().minus(60, java.time.temporal.ChronoUnit.DAYS)

        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val kafkamelding =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1", fnr = "fnr"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SENDT",
                        timestamp = OffsetDateTime.ofInstant(toManederSiden, ZoneId.of("UTC")),
                    ),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDINGSTATUS_TOPIC,
                    null,
                    "1",
                    kafkamelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            sykmeldingRepository.findBySykmeldingId("1")?.sisteHendelse()?.status shouldBeEqualTo HendelseStatus.SENDT_TIL_ARBEIDSGIVER
        }
    }

    @Test
    fun `burde ignorere hendelse fra kafka som har null value`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDINGSTATUS_TOPIC,
                    null,
                    "1",
                    null,
                ),
            ).get()

        await().atMost(Duration.ofSeconds(1)).untilAsserted {
            sykmeldingRepository.findBySykmeldingId("1")?.sisteHendelse()?.status shouldBeEqualTo HendelseStatus.APEN
        }
    }

    @Test
    fun `burde ikke lagre hendelse fra kafka dersom sykmelding ikke finnes`() {
        val kafkamelding =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1", fnr = "fnr"),
                event = lagSykmeldingStatusKafkaDTO(statusEvent = "SENDT"),
            )

        kafkaProducer
            .send(
                ProducerRecord(
                    SYKMELDINGSTATUS_TOPIC,
                    null,
                    "1",
                    kafkamelding.serialisertTilString(),
                ),
            ).get()

        await().atMost(Duration.ofSeconds(2)).untilAsserted {
            sykmeldingRepository.findBySykmeldingId("1") shouldBeEqualTo null
        }
    }
}
