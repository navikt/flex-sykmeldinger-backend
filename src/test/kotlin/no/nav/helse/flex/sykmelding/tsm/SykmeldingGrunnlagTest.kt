package no.nav.helse.flex.sykmelding.tsm

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.testdata.lagDigitalSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagUtenlandskSykmeldingGrunnlag
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be instance of`
import org.junit.jupiter.api.Test

class SykmeldingGrunnlagTest {
    @Test
    fun `burde deserialisere sykmelding`() {
        val opprinneligSykmelding = lagSykmeldingGrunnlag()
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

    @Test
    fun `mapper utdypendeSporsmal til utdypendeOpplysninger`() {
        val utdypendeSporsmal =
            listOf(
                UtdypendeSporsmal(
                    type = Sporsmalstype.MEDISINSK_OPPSUMMERING,
                    svar = "svar 1",
                    sporsmal = "sporsmal",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                    svar = "svar 2",
                    sporsmal = "sporsmal",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                    svar = "svar 3",
                    sporsmal = "sporsmal",
                ),
            )

        val expectedUtdypendeOpplysninger =
            mapOf(
                "6.3" to
                    mapOf(
                        "6.3.1" to
                            SporsmalSvar(
                                svar = "svar 1",
                                sporsmal = "sporsmal",
                                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                            ),
                        "6.3.2" to
                            SporsmalSvar(
                                svar = "svar 2",
                                sporsmal = "sporsmal",
                                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                            ),
                        "6.3.3" to
                            SporsmalSvar(
                                svar = "svar 3",
                                sporsmal = "sporsmal",
                                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                            ),
                    ),
            )

        toUtdypendeOpplysninger(utdypendeSporsmal) `should be equal to` expectedUtdypendeOpplysninger
    }
}
