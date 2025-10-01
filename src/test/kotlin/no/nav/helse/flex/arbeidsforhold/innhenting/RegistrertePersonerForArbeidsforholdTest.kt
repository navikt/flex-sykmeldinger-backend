package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.gateways.pdl.PdlIdent
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import org.amshove.kluent.`should be false`
import org.amshove.kluent.`should be true`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class RegistrertePersonerForArbeidsforholdTest : FakesTestOppsett() {
    @Autowired
    lateinit var registrertePersonerForArbeidsforhold: RegistrertePersonerForArbeidsforhold

    @Autowired
    lateinit var pdlClientFake: PdlClientFake

    @BeforeEach
    fun setup() {
        slettDatabase()
        pdlClientFake.reset()
    }

    @Test
    fun `burde returnere true når en person har sykmelding`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(pasient = lagPasient(fnr = "fnr"))))

        val result = registrertePersonerForArbeidsforhold.erPersonRegistrert("fnr")
        result.`should be true`()
    }

    @Test
    fun `burde returnere false når en person ikke har sykmelding`() {
        val result = registrertePersonerForArbeidsforhold.erPersonRegistrert("fnr")
        result.`should be false`()
    }

    @Test
    fun `burde returnere true dersom personen har en sykmelding med gammel fnr`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(pasient = lagPasient(fnr = "gammel-fnr"))))
        pdlClientFake.setIdentMedHistorikk(listOf(PdlIdent(gruppe = "FOLKEREGISTERIDENT", ident = "gammel-fnr")))

        val result = registrertePersonerForArbeidsforhold.erPersonRegistrert("ny-ident")
        result.`should be true`()
    }
}
