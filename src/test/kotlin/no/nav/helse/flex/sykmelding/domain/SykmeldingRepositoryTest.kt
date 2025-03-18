package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be null`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SykmeldingRepositoryTest : IntegrasjonTestOppsett() {
    @AfterEach
    fun rensDatabase() {
        super.slettDatabase()
    }

    @Test
    fun `burde lagre en sykmelding`() {
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
                sykmeldingGrunnlagOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
                meldingsinformasjon = lagMeldingsinformasjonEnkel(),
                validation = lagValidation(),
            )

        sykmeldingRepository.save(sykmelding)
        sykmeldingRepository.findBySykmeldingId("1").`should not be null`()
    }

    @Test
    fun `burde returnere lagret sykmelding`() {
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
                sykmeldingGrunnlagOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
                meldingsinformasjon = lagMeldingsinformasjonEnkel(),
                validation = lagValidation(),
            )

        val lagretSykmelding = sykmeldingRepository.save(sykmelding)
        lagretSykmelding.databaseId.`should not be null`()
        lagretSykmelding.setDatabaseIdsToNull() `should be equal to` sykmelding
    }

    @Test
    fun `burde oppdatere en sykmelding`() {
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
                sykmeldingGrunnlagOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
                meldingsinformasjon = lagMeldingsinformasjonEnkel(),
                validation = lagValidation(),
            )

        sykmeldingRepository.save(sykmelding)

        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1")
        val oppdatertSykmelding =
            hentetSykmelding
                ?.copy(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(id = "1").copy(
                            pasient = hentetSykmelding.sykmeldingGrunnlag.pasient.copy(fnr = "nyttFnr"),
                        ),
                    sykmeldingGrunnlagOppdatert = Instant.parse("2022-02-02T00:00:00.00Z"),
                ).`should not be null`()

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository.findBySykmeldingId("1").let {
            it.`should not be null`()
            it.sykmeldingGrunnlag.pasient.fnr `should be equal to` "nyttFnr"
            it.sykmeldingGrunnlagOppdatert `should be equal to` Instant.parse("2022-02-02T00:00:00.00Z")
        }
    }

    @Test
    fun `burde oppdatere en sykmelding uten å lagre dobbelt`() {
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
                sykmeldingGrunnlagOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
                meldingsinformasjon = lagMeldingsinformasjonEnkel(),
                validation = lagValidation(),
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
                hendelser =
                    listOf(
                        SykmeldingHendelse(
                            status = HendelseStatus.APEN,
                            opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                        ),
                    ),
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                sykmeldingGrunnlagOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
                meldingsinformasjon = lagMeldingsinformasjonEnkel(),
                validation = lagValidation(),
            )

        sykmeldingRepository.save(sykmelding)

        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1")
        val oppdatertSykmelding =
            hentetSykmelding
                ?.leggTilHendelse(
                    SykmeldingHendelse(status = HendelseStatus.APEN, opprettet = Instant.parse("2021-01-01T00:00:00.00Z")),
                ).`should not be null`()

        sykmeldingRepository.save(oppdatertSykmelding)

        sykmeldingRepository.findBySykmeldingId("1").let {
            it.`should not be null`()
            it.hendelser.size == 2
        }
    }

    @Test
    fun `burde lagre status med arbeidstaker info`() {
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

    @Test
    fun `sykmelding burde være lik når hentet`() {
        val sykmelding =
            Sykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
                hendelser =
                    listOf(
                        SykmeldingHendelse(
                            status = HendelseStatus.APEN,
                            opprettet = Instant.parse("2021-01-01T00:00:00.00Z").trimToMillisForOperativsystemForskjeller(),
                        ),
                    ),
                opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
                sykmeldingGrunnlagOppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
                meldingsinformasjon = lagMeldingsinformasjonEnkel(),
                validation = lagValidation(),
            )

        sykmeldingRepository.save(sykmelding)
        val hentetSykmelding = sykmeldingRepository.findBySykmeldingId("1").`should not be null`()

        hentetSykmelding.setDatabaseIdsToNull() `should be equal to` sykmelding
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
