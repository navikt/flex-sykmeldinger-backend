package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.FellesTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingRepositoryTest : FellesTestOppsett() {
    @Autowired
    lateinit var sykmeldingRepository: ISykmeldingRepository

    @AfterEach
    fun rensDatabase() {
        super.slettDatabase()
    }

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
        val oppdatertSykmelding =
            hentetSykmelding?.copy(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(id = "1").copy(
                        pasient = hentetSykmelding.sykmeldingGrunnlag.pasient.copy(fnr = "nyttFnr"),
                    ),
            ).`should not be null`()

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository.findBySykmeldingId("1").let {
            it.`should not be null`()
            it.sykmeldingGrunnlag.pasient.fnr `should be equal to` "nyttFnr"
        }
    }

    @Test
    fun `burde oppdatere en sykmelding uten å lagre dobbelt`() {
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
        val oppdatertSykmelding = hentetSykmelding.`should not be null`()

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository.findAll().size `should be equal to` 1
    }

    @Test
    fun `burde legge til en status i en sykmelding`() {
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
        val oppdatertSykmelding =
            hentetSykmelding?.leggTilStatus(SykmeldingStatus(status = "LEST")).`should not be null`()

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository.findBySykmeldingId("1").let {
            it.`should not be null`()
            it.statuser.size == 2
        }
    }

    @Test
    fun `sykmelding burde være lik når hentet`() {
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
        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1").`should not be null`()

        hentetSykmelding.setDatabaseIdsToNull() `should be equal to` sykmelding
    }

    private fun Sykmelding.setDatabaseIdsToNull(): Sykmelding {
        return this.copy(databaseId = null, statuser = this.statuser.map { it.copy(databaseId = null) })
    }
}
