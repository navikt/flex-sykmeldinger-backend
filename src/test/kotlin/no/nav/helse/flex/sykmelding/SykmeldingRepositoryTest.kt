package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.application.FiskerBrukerSvar
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

class SykmeldingRepositoryTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var sykmeldingHendelseDbRepository: SykmeldingHendelseDbRepository

    @AfterEach
    fun afterEach() {
        super.slettDatabase()
    }

    @Test
    fun `burde lagre en sykmelding`() {
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))

        sykmeldingRepository.save(sykmelding)
        sykmeldingRepository.findBySykmeldingId("1").`should not be null`()
    }

    @Test
    fun `burde lese og returnere lagret sykmelding med riktig data`() {
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                hendelser =
                    listOf(
                        SykmeldingHendelse(
                            status = HendelseStatus.APEN,
                            hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                            lokaltOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                        ),
                    ),
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                validation = lagValidation(),
                hendelseOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
                sykmeldingGrunnlagOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
                validationOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
            )

        val lagretSykmelding = sykmeldingRepository.save(sykmelding)
        lagretSykmelding.databaseId.`should not be null`()
        lagretSykmelding.setDatabaseIdsToNull() `should be equal to` sykmelding

        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1").`should not be null`()
        hentetSykmelding.setDatabaseIdsToNull() `should be equal to` sykmelding
    }

    @Test
    fun `burde oppdatere en sykmelding`() {
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "fnr")))

        sykmeldingRepository.save(sykmelding)

        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1").`should not be null`()
        val oppdatertSykmelding =
            hentetSykmelding.copy(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(id = "1").copy(
                        pasient = hentetSykmelding.sykmeldingGrunnlag.pasient.copy(fnr = "nyttFnr"),
                    ),
            )

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository.findBySykmeldingId("1").let {
            it.`should not be null`()
            it.sykmeldingGrunnlag.pasient.fnr `should be equal to` "nyttFnr"
        }
    }

    @Test
    fun `burde oppdatere en sykmelding uten å lagre dobbelt`() {
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))

        sykmeldingRepository.save(sykmelding)

        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1")
        val oppdatertSykmelding = hentetSykmelding.`should not be null`()

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository.findAll().size `should be equal to` 1
    }

    @Test
    fun `burde legge til en hendelse i en sykmelding`() {
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))

        sykmeldingRepository.save(sykmelding)

        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1").`should not be null`()
        val oppdatertSykmelding =
            hentetSykmelding
                .leggTilHendelse(
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        source = "TEST_SOURCE",
                        hendelseOpprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                        lokaltOpprettet = Instant.parse("2022-01-01T00:00:00.00Z"),
                    ),
                )

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository
            .findBySykmeldingId("1")
            .`should not be null`()
            .hendelser
            .shouldHaveSize(2)
            .last()
            .run {
                status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                source `should be equal to` "TEST_SOURCE"
                hendelseOpprettet `should be equal to` Instant.parse("2021-01-01T00:00:00.00Z")
                lokaltOpprettet `should be equal to` Instant.parse("2022-01-01T00:00:00.00Z")
            }
    }

    @TestFactory
    fun `burde lagre hendelse med tilleggsinfo`() =
        listOf(
            lagArbeidstakerTilleggsinfo(),
            lagArbeidsledigTilleggsinfo(),
            lagPermittertTilleggsinfo(),
            lagFiskerTilleggsinfo(),
            lagFrilanserTilleggsinfo(),
            lagNaringsdrivendeTilleggsinfo(),
            lagJordbrukerTilleggsinfo(),
            lagAnnetArbeidssituasjonTilleggsinfo(),
        ).map { tilleggsinfo ->
            DynamicTest.dynamicTest("burde lagre hendelse med tilleggsinfo for ${tilleggsinfo.type}") {
                val sykmeldingId = tilleggsinfo.type.name
                val hendelse =
                    lagSykmeldingHendelse(
                        tilleggsinfo = tilleggsinfo,
                    )
                val sykmelding =
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = sykmeldingId),
                        hendelser = listOf(hendelse),
                    )

                sykmeldingRepository.save(sykmelding)
                val lagretSykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId).shouldNotBeNull()
                val lagretTilleggsinfo = lagretSykmelding.sisteHendelse().tilleggsinfo
                lagretTilleggsinfo `should be equal to` tilleggsinfo
            }
        }

    @TestFactory
    fun `burde lagre hendelse med brukerSvar`() =
        listOf(
            lagArbeidstakerBrukerSvar(),
            lagArbeidsledigBrukerSvar(),
            lagPermittertBrukerSvar(),
            lagFiskerHyreBrukerSvar(),
            lagFiskerLottBrukerSvar(),
            lagFrilanserBrukerSvar(),
            lagNaringsdrivendeBrukerSvar(),
            lagJordbrukerBrukerSvar(),
            lagAnnetArbeidssituasjonBrukerSvar(),
        ).map { brukerSvar ->
            val caseId =
                if (brukerSvar is FiskerBrukerSvar) {
                    "${brukerSvar.type.name}-${brukerSvar.lottOgHyre.svar}"
                } else {
                    brukerSvar.type.name
                }

            DynamicTest.dynamicTest("burde lagre hendelse med brukerSvar for $caseId") {
                val sykmeldingId = caseId
                val hendelse =
                    lagSykmeldingHendelse(
                        brukerSvar = brukerSvar,
                    )
                val sykmelding =
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = sykmeldingId),
                        hendelser = listOf(hendelse),
                    )

                sykmeldingRepository.save(sykmelding)
                val lagretSykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId).shouldNotBeNull()
                val lagretBrukerSvar = lagretSykmelding.sisteHendelse().brukerSvar
                lagretBrukerSvar `should be equal to` brukerSvar
            }
        }

    @Test
    fun `burde hente alle ved person identer`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "1"))))
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", pasient = lagPasient(fnr = "2"))))
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "3", pasient = lagPasient(fnr = "3"))))

        sykmeldingRepository.findAllByPersonIdenter(PersonIdenter("1", listOf("2"))) shouldHaveSize 2
    }

    @Test
    fun `deleteBySykmeldingId burde slette bestemt sykmelding`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(),
                    ),
            ),
        )
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2")))

        sykmeldingRepository.deleteBySykmeldingId("1")

        sykmeldingRepository.findBySykmeldingId("1").shouldBeNull()
        sykmeldingRepository.findBySykmeldingId("2").shouldNotBeNull()
    }

    @Test
    fun `deleteBySykmeldingId burde slette sykmelding hendelser`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(),
                    ),
            ),
        )
        sykmeldingHendelseDbRepository.findAllBySykmeldingId("1").shouldNotBeEmpty()
        sykmeldingRepository.deleteBySykmeldingId("1")
        sykmeldingHendelseDbRepository.findAllBySykmeldingId("1").shouldBeEmpty()
    }

    @Test
    fun `delete burde slette bestemt sykmelding`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(),
                    ),
            ),
        )
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2")))

        val sykmelding1 = sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
        sykmeldingRepository.delete(sykmelding1)

        sykmeldingRepository.findBySykmeldingId("1").shouldBeNull()
        sykmeldingRepository.findBySykmeldingId("2").shouldNotBeNull()
    }

    @Test
    fun `delete burde slette sykmelding hendelser`() {
        sykmeldingRepository.save(
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                hendelser =
                    listOf(
                        lagSykmeldingHendelse(),
                    ),
            ),
        )
        sykmeldingHendelseDbRepository.findAllBySykmeldingId("1").shouldNotBeEmpty()
        val sykmelding1 = sykmeldingRepository.findBySykmeldingId("1").shouldNotBeNull()
        sykmeldingRepository.delete(sykmelding1)
        sykmeldingHendelseDbRepository.findAllBySykmeldingId("1").shouldBeEmpty()
    }

    private fun Sykmelding.setDatabaseIdsToNull(): Sykmelding =
        this.copy(
            databaseId = null,
            hendelser =
                this.hendelser.map {
                    it.copy(databaseId = null)
                },
        )
}
