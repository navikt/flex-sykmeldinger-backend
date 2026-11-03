package no.nav.helse.flex.sykmeldinghendelse

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykmeldingStatusKafkaProducerFake
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingHendelsePublisererTest : FakesTestOppsett() {
    @Autowired
    private lateinit var sykmeldingHendelsePubliserer: SykmeldingHendelsePubliserer

    @Autowired
    private lateinit var sykmeldingStatusKafkaProducer: SykmeldingStatusKafkaProducerFake

    @AfterEach
    fun reset() {
        sykmeldingStatusKafkaProducer.reset()
    }

    @Test
    fun `burde publisere hendelse`() {
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))

        sykmeldingHendelsePubliserer.publiserSisteHendelse(sykmelding)

        sykmeldingStatusKafkaProducer.sendteSykmeldingStatuser().shouldHaveSize(1).first().run {
            event.sykmeldingId shouldBe "1"
        }
    }

    @Nested
    inner class Funksjoner {
        @Test
        fun `burde sammenstille data til SykmeldingStatusKafkaMessageDTO`() {
            val sykmeldingStatusKafkaDTO: SykmeldingStatusKafkaDTO = lagSykmeldingStatusKafkaMessageDTO().event
            val sammenstillSykmeldingStatusKafkaMessageDTO =
                SykmeldingHendelsePubliserer.sammenstillSykmeldingStatusKafkaMessageDTO(
                    fnr = "fnr",
                    sykmeldingStatusKafkaDTO = sykmeldingStatusKafkaDTO,
                )
            sammenstillSykmeldingStatusKafkaMessageDTO.kafkaMetadata.shouldNotBeNull()
            sammenstillSykmeldingStatusKafkaMessageDTO.event.brukerSvar.shouldNotBeNull()
        }
    }
}
