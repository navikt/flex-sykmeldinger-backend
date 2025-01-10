package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.FellesTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingRepositoryTest : FellesTestOppsett() {
    @Autowired
    lateinit var sykmeldingRepository: ISykmeldingRepository

    @Test
    fun `burde lagre en sykmelding`() {
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                statuser =
                    listOf(
                        SykmeldingStatus(
                            status = "NY",
                        ),
                    ),
            )

        sykmeldingRepository.save(sykmelding)
        sykmeldingRepository.findBySykmeldingId("1").`should not be null`()
    }

    @Test
    fun `burde oppdatere en sykmelding`() {
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                statuser =
                    listOf(
                        SykmeldingStatus(
                            status = "NY",
                        ),
                    ),
            )

        sykmeldingRepository.save(sykmelding)

        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1")
        val oppdatertSykmelding = hentetSykmelding?.copy(
            sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1").copy(
                pasient = hentetSykmelding.sykmeldingGrunnlag.pasient.copy(fnr = "nyttFnr")
            )
        ).`should not be null`()

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository.findBySykmeldingId("1").let {
            it.`should not be null`()
            it.sykmeldingGrunnlag.pasient.fnr `should be equal to` "nyttFnr"
        }
    }

    @Test
    fun `burde legge til en status i en sykmelding`() {
        error("")
    }

    @Test
    fun `burde hente en sykmelding`() {
        error("")
    }
}
