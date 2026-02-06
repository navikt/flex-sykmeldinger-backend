package no.nav.helse.flex.sykmelding.tsm

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.testdata.lagDigitalSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagUtenlandskSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagXMLSykmeldingGrunnlag
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be instance of`
import org.junit.jupiter.api.Test

class SykmeldingGrunnlagTest {
    @Test
    fun `burde serialisere og deserialisere XML sykmelding`() {
        val opprinneligSykmelding = lagXMLSykmeldingGrunnlag()
        val sykmeldingSerialisert = opprinneligSykmelding.serialisertTilString()

        val sykmelding: ISykmeldingGrunnlag = objectMapper.readValue(sykmeldingSerialisert)
        sykmelding `should be instance of` XMLSykmeldingGrunnlag::class
        sykmelding `should be equal to` opprinneligSykmelding
    }

    @Test
    fun `burde serialisere og deserialisere utenlandsk sykmelding`() {
        val opprinneligUtenlandskSykmelding = lagUtenlandskSykmeldingGrunnlag()
        val utenlandskSykmeldingSerialisert = opprinneligUtenlandskSykmelding.serialisertTilString()

        val sykmelding: ISykmeldingGrunnlag = objectMapper.readValue(utenlandskSykmeldingSerialisert)
        sykmelding `should be instance of` UtenlandskSykmeldingGrunnlag::class
        sykmelding `should be equal to` opprinneligUtenlandskSykmelding
    }

    @Test
    fun `burde serialisere og deserialisere digital sykmelding`() {
        val opprinneligDigitalSykmelding = lagDigitalSykmeldingGrunnlag()
        val digitalSykmeldingSerialisert = opprinneligDigitalSykmelding.serialisertTilString()

        val sykmelding: ISykmeldingGrunnlag = objectMapper.readValue(digitalSykmeldingSerialisert)
        sykmelding `should be instance of` DigitalSykmeldingGrunnlag::class
        sykmelding `should be equal to` opprinneligDigitalSykmelding
    }
}
