package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.Instant
import java.time.temporal.ChronoUnit

class SykmeldingRepositoryTest : IntegrasjonTestOppsett() {
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
                            opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                        ),
                    ),
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                meldingsinformasjon = lagMeldingsinformasjonEnkel(),
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
                    SykmeldingHendelse(status = HendelseStatus.APEN, opprettet = Instant.parse("2021-01-01T00:00:00.00Z")),
                )

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository
            .findBySykmeldingId("1")
            .`should not be null`()
            .hendelser.size `should be equal to` 2
    }

    @Test
    fun `burde lagre hendelse med arbeidstaker info`() {
        val hendelse =
            lagSykmeldingHendelse(
                status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                arbeidstakerInfo =
                    lagArbeidstakerInfo(
                        arbeidsgiver =
                            lagArbeidsgiver(
                                orgnummer = "orgnummer",
                                juridiskOrgnummer = "juridiskOrgnummer",
                                orgnavn = "orgnavn",
                                erAktivtArbeidsforhold = true,
                                narmesteLeder = NarmesteLeder(navn = "narmesteLederNavn"),
                            ),
                    ),
            )
        val sykmelding =
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                statuser = listOf(hendelse),
            )

        val lagretSykmelding = sykmeldingRepository.save(sykmelding)
        val arbeidstakerInfo = lagretSykmelding.sisteHendelse().arbeidstakerInfo
        arbeidstakerInfo `should be equal to`
            ArbeidstakerInfo(
                Arbeidsgiver(
                    orgnummer = "orgnummer",
                    juridiskOrgnummer = "juridiskOrgnummer",
                    orgnavn = "orgnavn",
                    erAktivtArbeidsforhold = true,
                    narmesteLeder = NarmesteLeder(navn = "narmesteLederNavn"),
                ),
            )
    }

    @TestFactory
    fun `burde lagre hendelse med tilleggsinfo`() =
        listOf(
            byggArbeidstakerTilleggsinfo(),
            byggArbeidsledigTilleggsinfo(),
            byggPermittertTilleggsinfo(),
            byggFiskerTilleggsinfo(),
            byggFrilanserTilleggsinfo(),
            byggNaringsdrivendeTilleggsinfo(),
            byggJordbrukerTilleggsinfo(),
            byggAnnetArbeidssituasjonTilleggsinfo(),
        ).mapIndexed { i, tilleggsinfo ->
            DynamicTest.dynamicTest("burde lagre hendelse med tilleggsinfo for ${tilleggsinfo.arbeidssituasjon}") {
                val sykmeldingId = "$i"
                val hendelse =
                    lagSykmeldingHendelse(
                        tilleggsinfo = tilleggsinfo,
                    )
                val sykmelding =
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = sykmeldingId),
                        statuser = listOf(hendelse),
                    )

                sykmeldingRepository.save(sykmelding)
                val lagretSykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId).shouldNotBeNull()
                val lagretTilleggsinfo = lagretSykmelding.sisteHendelse().tilleggsinfo
                lagretTilleggsinfo `should be equal to` tilleggsinfo
            }
        }

    @Test
    fun `burde hente alle ved person identer`() {
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", pasient = lagPasient(fnr = "1"))))
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", pasient = lagPasient(fnr = "2"))))
        sykmeldingRepository.save(lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "3", pasient = lagPasient(fnr = "3"))))

        sykmeldingRepository.findAllByPersonIdenter(PersonIdenter("1", listOf("2"))) shouldHaveSize 2
    }

    private fun Sykmelding.setDatabaseIdsToNull(): Sykmelding =
        this.copy(
            databaseId = null,
            hendelser =
                this.hendelser.map {
                    it.copy(databaseId = null)
                },
        )

    fun Instant.trimToMillisForOperativsystemForskjeller(): Instant = truncatedTo(ChronoUnit.MICROS)
}
