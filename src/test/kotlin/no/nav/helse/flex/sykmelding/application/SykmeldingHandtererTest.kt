package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingHendelse
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingHandtererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingHandterer: SykmeldingHandterer

    @AfterEach
    fun cleanUp() {
        slettDatabase()
    }

    @Nested
    inner class SendSykmeldingTilArbeidsgiver {
        @Test
        fun `burde lagre hendelse`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            sykmeldingHandterer.sendSykmeldingTilArbeidsgiver(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                arbeidsgiverOrgnummer = "orgnr",
                sporsmalSvar = null,
            )

            sykmeldingRepository
                .findBySykmeldingId("1")
                .shouldNotBeNull()
                .also { it.statuser shouldHaveSize 2 }
                .also { it.sisteStatus().status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER }
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            val sykmelding =
                sykmeldingHandterer.sendSykmeldingTilArbeidsgiver(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidsgiverOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )

            sykmelding
                .sisteStatus()
                .status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                ),
            )

            invoking {
                sykmeldingHandterer.sendSykmeldingTilArbeidsgiver(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidsgiverOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )
            } `should throw` UgyldigSykmeldingStatusException::class
        }
    }

    @Nested
    inner class SendSykmeldingTilNav {
        @Test
        fun `burde lagre hendelse`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            sykmeldingHandterer.sendSykmeldingTilNav(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                arbeidsledigFraOrgnummer = null,
                sporsmalSvar = null,
            )

            sykmeldingRepository
                .findBySykmeldingId("1")
                .shouldNotBeNull()
                .also { it.statuser shouldHaveSize 2 }
                .also { it.sisteStatus().status `should be equal to` HendelseStatus.SENDT_TIL_NAV }
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            val sykmelding =
                sykmeldingHandterer.sendSykmeldingTilNav(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidsledigFraOrgnummer = null,
                    sporsmalSvar = null,
                )

            sykmelding
                .sisteStatus()
                .status `should be equal to` HendelseStatus.SENDT_TIL_NAV
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                ),
            )

            invoking {
                sykmeldingHandterer.sendSykmeldingTilNav(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidsledigFraOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )
            } `should throw` UgyldigSykmeldingStatusException::class
        }
    }

    @Nested
    inner class AvbrytSykmelding {
        @Test
        fun `burde lagre hendelse`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            sykmeldingHandterer.avbrytSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
            )

            sykmeldingRepository
                .findBySykmeldingId("1")
                .shouldNotBeNull()
                .also { it.statuser shouldHaveSize 2 }
                .also { it.sisteStatus().status `should be equal to` HendelseStatus.AVBRUTT }
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                ),
            )

            val sykmelding =
                sykmeldingHandterer.avbrytSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                )

            sykmelding
                .sisteStatus()
                .status `should be equal to` HendelseStatus.AVBRUTT
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                ),
            )

            invoking {
                sykmeldingHandterer.avbrytSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                )
            } `should throw` UgyldigSykmeldingStatusException::class
        }
    }

    @Nested
    inner class BekreftAvvistSykmelding {
        @Test
        fun `burde lagre hendelse`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            sykmeldingHandterer.bekreftAvvistSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
            )

            sykmeldingRepository
                .findBySykmeldingId("1")
                .shouldNotBeNull()
                .also { it.statuser shouldHaveSize 2 }
                .also { it.sisteStatus().status `should be equal to` HendelseStatus.BEKREFTET_AVVIST }
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                ),
            )

            val sykmelding =
                sykmeldingHandterer.bekreftAvvistSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                )

            sykmelding
                .sisteStatus()
                .status `should be equal to` HendelseStatus.BEKREFTET_AVVIST
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                ),
            )

            invoking {
                sykmeldingHandterer.bekreftAvvistSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                )
            } `should throw` UgyldigSykmeldingStatusException::class
        }
    }
}
