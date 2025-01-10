package no.nav.helse.flex.sykmelding.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.amshove.kluent.`should be instance of`
import org.junit.jupiter.api.Test

class SykmeldingTest {
    @Test
    fun `burde deserialisere sykmelding`() {
        val sykmeldingSerialisert = lagSykmeldingGrunnlag().serialisertTilString()

        val sykmelding: ISykmeldingGrunnlag = objectMapper.readValue(sykmeldingSerialisert)
        sykmelding `should be instance of` SykmeldingGrunnlag::class
    }

    @Test
    fun `burde serialisere og deserialisere utenlandsk sykmelding`() {
        val utenlandskSykmeldingSerialisert = lagUtenlandskSykmeldingGrunnlag().serialisertTilString()

        val sykmelding: ISykmeldingGrunnlag = objectMapper.readValue(utenlandskSykmeldingSerialisert)
        sykmelding `should be instance of` UtenlandskSykmeldingGrunnlag::class
    }
}
