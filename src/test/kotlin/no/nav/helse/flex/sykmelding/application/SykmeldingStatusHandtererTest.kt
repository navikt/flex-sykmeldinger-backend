package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.producers.sykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingHendelse
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaMessageDTO
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingStatusHandtererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusHandterer: SykmeldingStatusHandterer

    @AfterEach
    fun cleanUp() {
        slettDatabase()
    }

    @Test
    fun `burde lagre hendelse på sykmelding`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status = lagSykmeldingStatusKafkaMessageDTO(sykmeldingId = "1")
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be true`()
        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        sykmelding.`should not be null`()
        sykmelding.hendelser.size shouldBeEqualTo 2
        sykmelding.sisteHendelse().status shouldBeEqualTo HendelseStatus.SENDT_TIL_ARBEIDSGIVER
    }

    @Test
    fun `burde ikke lagre hendelse dersom sykmelding ikke finnes`() {
        val status = lagSykmeldingStatusKafkaMessageDTO(sykmeldingId = "1")
        sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be false`()
        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        sykmelding.`should be null`()
    }

    @Test
    fun `burde ikke kunne endre status fra SENDT_TIL_ARBEIDSGIVER til SENDT`() {
        sykmeldingRepository.save(
            lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))
                .leggTilHendelse(
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                    ),
                ),
        )
        val status = lagSykmeldingStatusKafkaMessageDTO(sykmeldingId = "1", statusEvent = "SENDT")
        invoking {
            sykmeldingStatusHandterer.handterSykmeldingStatus(status).`should be false`()
        } `should throw` UgyldigSykmeldingStatusException::class
    }

    @Test
    fun `burde sammenstille data til SykmeldingStatusKafkaMessageDTO`() {
        val sykmeldingStatusKafkaDTO: SykmeldingStatusKafkaDTO = lagSykmeldingStatusKafkaMessageDTO().event
        val sammenstillSykmeldingStatusKafkaMessageDTO =
            sykmeldingStatusHandterer.sammenstillSykmeldingStatusKafkaMessageDTO(
                fnr = "fnr",
                sykmeldingStatusKafkaDTO = sykmeldingStatusKafkaDTO,
            )
        sammenstillSykmeldingStatusKafkaMessageDTO.kafkaMetadata.`should not be null`()
        sammenstillSykmeldingStatusKafkaMessageDTO.event.brukerSvar.`should not be null`()
    }

    @Disabled
    @Test
    fun `burde publisere på retry dersom sykmelding ikke finnes`() {
        val status = lagSykmeldingStatusKafkaMessageDTO(sykmeldingId = "1")
        sykmeldingStatusHandterer.handterSykmeldingStatus(status)
    }
}
