package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.AdvisoryLockFake
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.springframework.beans.factory.annotation.Autowired
import java.util.concurrent.TimeUnit

class SykmeldingStatusHandtererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusHandterer: SykmeldingStatusHandterer

    @Autowired
    lateinit var sykmeldingStatusBuffer: SykmeldingStatusBuffer

    @Autowired
    lateinit var advisoryLockFake: AdvisoryLockFake

    @AfterEach
    fun cleanUp() {
        slettDatabase()
    }

    @Test
    fun `burde lagre hendelse på sykmelding`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be true`()
        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        sykmelding.`should not be null`()
        sykmelding.hendelser.size shouldBeEqualTo 2
        sykmelding.sisteHendelse().status shouldBeEqualTo HendelseStatus.SENDT_TIL_ARBEIDSGIVER
    }

    @Test
    fun `burde ikke lagre hendelse dersom sykmelding ikke finnes`() {
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be false`()
        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        sykmelding.`should be null`()
    }

    @Test
    fun `burde ignorere APEN status`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            hendelser.shouldHaveSize(1)
        }
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "APEN",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            hendelser.shouldHaveSize(1)
        }
    }

    @Test
    fun `burde returnere true ved APEN status`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "APEN",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).shouldBeTrue()
    }

    @Test
    fun `burde ignorere SLETTET status`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            hendelser.shouldHaveSize(1)
        }
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SLETTET",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull().run {
            hendelser.shouldHaveSize(1)
        }
    }

    @Test
    fun `burde returnere true ved SLETTET status`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SLETTET",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be true`()
    }

    @Test
    fun `burde kun slette spesifikk sykmelding ved status SLETTET`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
                event =
                    lagSykmeldingStatusKafkaDTO(
                        statusEvent = "SLETTET",
                    ),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        sykmeldingRepository.findBySykmeldingId("2").shouldNotBeNull()
    }

    @Test
    fun `burde respektere regler for status endring`() {
        sykmeldingRepository.save(
            lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))
                .leggTilHendelse(
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                    ),
                ),
        )
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        invoking {
            sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be false`()
        } `should throw` UgyldigSykmeldingStatusException::class
    }

    @Test
    fun `burde sammenstille data til SykmeldingStatusKafkaMessageDTO`() {
        val sykmeldingStatusKafkaDTO: SykmeldingStatusKafkaDTO = lagSykmeldingStatusKafkaMessageDTO().event
        val sammenstillSykmeldingStatusKafkaMessageDTO =
            SykmeldingStatusHandterer.sammenstillSykmeldingStatusKafkaMessageDTO(
                fnr = "fnr",
                sykmeldingStatusKafkaDTO = sykmeldingStatusKafkaDTO,
            )
        sammenstillSykmeldingStatusKafkaMessageDTO.kafkaMetadata.`should not be null`()
        sammenstillSykmeldingStatusKafkaMessageDTO.event.brukerSvar.`should not be null`()
    }

    @Test
    fun `burde buffre status dersom sykmelding ikke finnes`() {
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        val buffredeHendelser = sykmeldingStatusBuffer.kikkPaaAlleFor("1")
        buffredeHendelser.size shouldBeEqualTo 1
        buffredeHendelser.first().kafkaMetadata.sykmeldingId shouldBeEqualTo "1"
    }

    @Test
    fun `burde ikke buffre status dersom sykmelding finnes`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status =
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            )
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
        val buffredeHendelser = sykmeldingStatusBuffer.kikkPaaAlleFor("1")
        buffredeHendelser.size shouldBeEqualTo 0
    }

    @Test
    fun `burde ikke lagre status hvis hendelse eksisterer på sykmeldingen`() {
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))
        sykmeldingRepository.save(sykmelding)
        val status = lagSykmeldingStatusKafkaMessageDTO(kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"))
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be true`()
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be false`()
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    fun `handterSykmeldingStatus og prosesserSykmeldingStatuserFraBuffer burde synkronisere ved buffer lås`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        sykmeldingStatusHandterer.handterSykmeldingStatus(
            lagSykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = lagKafkaMetadataDTO(sykmeldingId = "1"),
            ),
        )
        advisoryLockFake.lockCount() `should be equal to` 1
        sykmeldingStatusHandterer.prosesserSykmeldingStatuserFraBuffer(sykmeldingId = "1")
        advisoryLockFake.lockCount() `should be equal to` 2
    }
}
