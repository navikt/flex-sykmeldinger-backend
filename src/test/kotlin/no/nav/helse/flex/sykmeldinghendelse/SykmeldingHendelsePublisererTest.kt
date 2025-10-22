package no.nav.helse.flex.sykmeldinghendelse

import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykmeldingStatusKafkaProducerFake
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagXMLSykmeldingGrunnlag
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
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
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagXMLSykmeldingGrunnlag(id = "1"))

        sykmeldingHendelsePubliserer.publiserSisteHendelse(sykmelding)

        sykmeldingStatusKafkaProducer.sendteSykmeldingStatuser().shouldHaveSize(1).first().run {
            event.sykmeldingId shouldBeEqualTo "1"
        }
    }
}
