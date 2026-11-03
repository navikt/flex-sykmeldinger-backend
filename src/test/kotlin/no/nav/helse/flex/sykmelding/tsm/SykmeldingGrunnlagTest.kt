package no.nav.helse.flex.sykmelding.tsm

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helse.flex.testdata.lagDigitalSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagUtenlandskSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagXMLSykmeldingGrunnlag
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.readValue

class SykmeldingGrunnlagTest {
    @Test
    fun `burde serialisere og deserialisere XML sykmelding`() {
        val opprinneligSykmelding = lagXMLSykmeldingGrunnlag()
        val sykmeldingSerialisert = opprinneligSykmelding.serialisertTilString()

        val sykmelding: SykmeldingGrunnlag = objectMapper.readValue(sykmeldingSerialisert)
        sykmelding.shouldBeInstanceOf(XMLSykmeldingGrunnlag::class)
        sykmelding shouldBe opprinneligSykmelding
    }

    @Test
    fun `burde serialisere og deserialisere utenlandsk sykmelding`() {
        val opprinneligUtenlandskSykmelding = lagUtenlandskSykmeldingGrunnlag()
        val utenlandskSykmeldingSerialisert = opprinneligUtenlandskSykmelding.serialisertTilString()

        val sykmelding: SykmeldingGrunnlag = objectMapper.readValue(utenlandskSykmeldingSerialisert)
        sykmelding.shouldBeInstanceOf(UtenlandskSykmeldingGrunnlag::class)
        sykmelding shouldBe opprinneligUtenlandskSykmelding
    }

    @Test
    fun `burde serialisere og deserialisere digital sykmelding`() {
        val opprinneligDigitalSykmelding = lagDigitalSykmeldingGrunnlag()
        val digitalSykmeldingSerialisert = opprinneligDigitalSykmelding.serialisertTilString()

        val sykmelding: SykmeldingGrunnlag = objectMapper.readValue(digitalSykmeldingSerialisert)
        sykmelding.shouldBeInstanceOf(DigitalSykmeldingGrunnlag::class)
        sykmelding shouldBe opprinneligDigitalSykmelding
    }
}
