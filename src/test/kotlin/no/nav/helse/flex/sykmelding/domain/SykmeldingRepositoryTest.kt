package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.FellesTestOppsett
import org.amshove.kluent.`should not be null`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingRepositoryTest : FellesTestOppsett() {

    @Autowired
    lateinit var sykmeldingRepository: ISykmeldingRepository

    @Test
    fun `burde lagre en sykmelding`() {
        val sykmelding = Sykmelding(
            sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
            statuser = listOf(
                SykmeldingStatus(
                    status = "NY",
                )
            )
        )

        sykmeldingRepository.save(sykmelding)
        sykmeldingRepository.findBySykmeldingId("1").`should not be null`()
    }

    @Test
    fun `burde oppdatere en sykmelding`() {
        error("")
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
