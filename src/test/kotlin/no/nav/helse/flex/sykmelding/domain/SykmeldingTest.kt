package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.objectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.serialisertTilString
import org.amshove.kluent.`should be instance of`
import org.junit.jupiter.api.Test

class SykmeldingTest {

    @Test
    fun `burde deserialisere sykmelding`() {
        val sykmeldingSerialisert = lagSykmelding().serialisertTilString()

        val sykmelding: ISykmelding = objectMapper.readValue(sykmeldingSerialisert)
        sykmelding `should be instance of` Sykmelding::class
    }

    @Test
    fun `burde serialisere og deserialisere utenlandsk sykmelding`() {
        val utenlandskSykmeldingSerialisert = lagUtenlandskSykmelding().serialisertTilString()

        val sykmelding: ISykmelding = objectMapper.readValue(utenlandskSykmeldingSerialisert)
        sykmelding `should be instance of` UtenlandskSykmelding::class
    }
}
