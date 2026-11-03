package no.nav.helse.flex.sykmelding

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.helse.flex.sykmelding.tsm.AvsenderSystem
import no.nav.helse.flex.sykmelding.tsm.AvsenderSystemNavn
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingMetadata
import no.nav.helse.flex.testdata.lagValidation
import org.junit.jupiter.api.Test

class SykmeldingTest {
    @Test
    fun `burde ikke være avvist ved validation OK`() {
        val sykmelding = lagSykmelding()
        sykmelding.erAvvist shouldBe false
    }

    @Test
    fun `burde være avvist ved validation INVALID`() {
        val sykmelding = lagSykmelding(validation = lagValidation(status = RuleType.INVALID))
        sykmelding.erAvvist shouldBe true
    }

    @Test
    fun `burde ikke være avvist ved validation PENDING`() {
        val sykmelding = lagSykmelding(validation = lagValidation(status = RuleType.INVALID))
        sykmelding.erAvvist shouldBe true
    }

    @Test
    fun `burde være egenmeldt ved avsendersystem type EGENMELDT`() {
        val sykmelding =
            lagSykmelding(
                lagSykmeldingGrunnlag(
                    metadata =
                        lagSykmeldingMetadata(
                            avsenderSystem =
                                AvsenderSystem(
                                    navn = AvsenderSystemNavn.EGENMELDT,
                                    versjon = "1.0",
                                ),
                        ),
                ),
            )
        sykmelding.sykmeldingGrunnlag.metadata.avsenderSystem.navn shouldBe AvsenderSystemNavn.EGENMELDT
    }

    @Test
    fun `burde ikke være egenmeldt vanligvis`() {
        val sykmelding = lagSykmelding()
        sykmelding.sykmeldingGrunnlag.metadata.avsenderSystem.navn shouldNotBe AvsenderSystemNavn.EGENMELDT
    }
}
