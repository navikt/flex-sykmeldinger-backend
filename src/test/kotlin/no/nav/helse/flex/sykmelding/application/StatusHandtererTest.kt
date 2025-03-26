package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagStatus
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class StatusHandtererTest : FakesTestOppsett() {
    @Autowired
    lateinit var statusHandterer: StatusHandterer

    @AfterEach
    fun cleanUp() {
        slettDatabase()
    }

    @Test
    fun `burde lagre hendelse på sykmelding`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1")))
        val status = lagStatus(sykmeldingId = "1")
        statusHandterer.handterStatus(status)
        val sykmelding = sykmeldingRepository.findBySykmeldingId(status.kafkaMetadata.sykmeldingId)
        sykmelding.`should not be null`()
        sykmelding.hendelser.size shouldBeEqualTo 2
        sykmelding.sisteHendelse().status shouldBeEqualTo HendelseStatus.SENDT_TIL_ARBEIDSGIVER
    }
}
