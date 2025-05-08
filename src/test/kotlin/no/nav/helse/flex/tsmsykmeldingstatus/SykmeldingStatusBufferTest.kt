package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagKafkaMetadataDTO
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaDTO
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaMessageDTO
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.OffsetDateTime

class SykmeldingStatusBufferTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusBuffer: SykmeldingStatusBuffer

    @AfterEach
    fun afterEach() {
        sykmeldingStatusBufferRepository.deleteAll()
        advisoryLock.reset()
    }

    @Test
    fun `leggTil burde legge til sykmeldinghendelse`() {
        val kafkaMelding =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event = lagSykmeldingStatusKafkaDTO(statusEvent = "TEST_STATUS"),
            )
        sykmeldingStatusBuffer.leggTil(kafkaMelding)
        val lagredeHendelser = sykmeldingStatusBuffer.kikkPaaAlleFor("1")
        lagredeHendelser shouldHaveSize 1
        lagredeHendelser.first() shouldBeEqualTo kafkaMelding
    }

    @Test
    fun `leggTil burde låse buffer`() {
        sykmeldingStatusBuffer.leggTil(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
        )
        advisoryLock.hasLock("SykmeldingHendelseBuffer", "1").shouldBeTrue()
    }

    @Test
    fun `kikkPaaAlleFor burde returnere alle for sykmeldingId`() {
        listOf(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "2"),
            ),
        ).forEach {
            sykmeldingStatusBuffer.leggTil(it)
        }
        val lagredeHendelser = sykmeldingStatusBuffer.kikkPaaAlleFor("1")
        lagredeHendelser shouldHaveSize 2
        lagredeHendelser.forEach {
            it.kafkaMetadata.sykmeldingId shouldBeEqualTo "1"
        }
    }

    @Test
    fun `kikkPaaAlleFor burde ikke låse buffer`() {
        sykmeldingStatusBuffer.leggTil(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
        )
        advisoryLock.reset()
        sykmeldingStatusBuffer.kikkPaaAlleFor("1")
        advisoryLock.hasLocks().shouldBeFalse()
    }

    @Test
    fun `prosesserAlleFor burde hente alle for sykmeldingId`() {
        listOf(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "2"),
            ),
        ).forEach { sykmeldingStatusBuffer.leggTil(it) }

        val hendelser = sykmeldingStatusBuffer.prosesserAlleFor("1")
        hendelser shouldHaveSize 2
        hendelser.forEach {
            it.kafkaMetadata.sykmeldingId shouldBeEqualTo "1"
        }
    }

    @Test
    fun `prosesserAlleFor burde slette alle for sykmeldingId`() {
        listOf(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "2"),
            ),
        ).forEach { sykmeldingStatusBuffer.leggTil(it) }

        sykmeldingStatusBuffer.prosesserAlleFor("1")
        val resterendeHendelser = sykmeldingStatusBuffer.kikkPaaAlleFor("1")
        resterendeHendelser shouldHaveSize 0
        val andreHendelser = sykmeldingStatusBuffer.kikkPaaAlleFor("2")
        andreHendelser shouldHaveSize 1
    }

    @Test
    fun `prosesserAlleFor burde returnere hendelser i rekkefølge`() {
        val førsteTidspunkt = OffsetDateTime.parse("2023-01-01T00:00:00Z")
        val andreTidspunkt = OffsetDateTime.parse("2024-01-01T00:00:00Z")
        val tredjeTidspunkt = OffsetDateTime.parse("2025-01-01T00:00:00Z")

        listOf(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata =
                    lagKafkaMetadataDTO(
                        sykmeldingId = "1",
                        timestamp = førsteTidspunkt,
                    ),
            ),
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata =
                    lagKafkaMetadataDTO(
                        sykmeldingId = "1",
                        timestamp = tredjeTidspunkt,
                    ),
            ),
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata =
                    lagKafkaMetadataDTO(
                        sykmeldingId = "1",
                        timestamp = andreTidspunkt,
                    ),
            ),
        ).forEach { sykmeldingStatusBuffer.leggTil(it) }

        val hendelser = sykmeldingStatusBuffer.prosesserAlleFor("1")
        hendelser shouldHaveSize 3
        hendelser.map { it.kafkaMetadata.timestamp } shouldBeEqualTo
            listOf(
                førsteTidspunkt,
                andreTidspunkt,
                tredjeTidspunkt,
            )
    }

    @Test
    fun `prosesserAlleFor burde låse buffer`() {
        sykmeldingStatusBuffer.leggTil(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
        )
        advisoryLock.reset()
        sykmeldingStatusBuffer.prosesserAlleFor("1")
        advisoryLock.hasLocks().shouldBeTrue()
    }
}
