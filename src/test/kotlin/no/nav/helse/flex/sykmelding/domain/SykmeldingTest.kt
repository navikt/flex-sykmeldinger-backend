package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.testdata.lagMeldingsinformasjonEgenmeldt
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagValidation
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test

class SykmeldingTest {
    @Test
    fun `burde ikke være avvist ved validation OK`() {
        val sykmelding = lagSykmelding()
        sykmelding.erAvvist `should be equal to` false
    }

    @Test
    fun `burde være avvist ved validation INVALID`() {
        val sykmelding = lagSykmelding(validation = lagValidation(status = RuleType.INVALID))
        sykmelding.erAvvist `should be equal to` true
    }

    @Test
    fun `burde ikke være avvist ved validation PENDING`() {
        val sykmelding = lagSykmelding(validation = lagValidation(status = RuleType.INVALID))
        sykmelding.erAvvist `should be equal to` true
    }

    @Test
    fun `burde være egenmeldt ved meldingsinformasjon type EGENMELDT`() {
        val sykmelding = lagSykmelding(meldingsinformasjon = lagMeldingsinformasjonEgenmeldt())
        sykmelding.erEgenmeldt `should be equal to` true
    }

    @Test
    fun `burde ikke være egenmeldt vanligvis`() {
        val sykmelding = lagSykmelding()
        sykmelding.erEgenmeldt `should be equal to` false
    }
}
