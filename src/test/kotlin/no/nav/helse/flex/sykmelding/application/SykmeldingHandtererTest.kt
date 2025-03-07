package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykmeldingProducerFake
import no.nav.helse.flex.testdata.*
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

    @Autowired
    lateinit var sykmeldingProducer: SykmeldingProducerFake

    @AfterEach
    fun cleanUp() {
        slettDatabase()
        sykmeldingProducer.reset()
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

        @Test
        fun `burde sende sykmelding med hendelse til producer`() {
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

            sykmeldingProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
                .first()
                .sisteStatus()
                .status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
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
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.SENDT_TIL_ARBEIDSGIVER.name)
                .shouldContainIgnoringCase(sisteStatus.name)
        }

        @Test
        fun `burde ikke gå dersom sykmelding er avvist`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    validation = lagValidation(status = RuleType.INVALID),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            invoking {
                sykmeldingHandterer.sendSykmeldingTilArbeidsgiver(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidsgiverOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase("avvist")
        }

        @Test
        fun `burde ikke gå dersom sykmelding er egenmeldt`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    meldingsinformasjon = lagMeldingsinformasjonEgenmeldt(),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            invoking {
                sykmeldingHandterer.sendSykmeldingTilArbeidsgiver(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidsgiverOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase("egenmeldt")
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

        @Test
        fun `burde sende sykmelding med hendelse til producer`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            sykmeldingHandterer.sendSykmeldingTilNav(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                arbeidsledigFraOrgnummer = null,
                sporsmalSvar = null,
            )

            sykmeldingProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
                .first()
                .sisteStatus()
                .status `should be equal to` HendelseStatus.SENDT_TIL_NAV
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
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.SENDT_TIL_NAV.name)
                .shouldContainIgnoringCase(sisteStatus.name)
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

        @Test
        fun `burde sende sykmelding med hendelse til producer`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            sykmeldingHandterer.avbrytSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
            )

            sykmeldingProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
                .first()
                .sisteStatus()
                .status `should be equal to` HendelseStatus.AVBRUTT
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
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.AVBRUTT.name)
                .shouldContainIgnoringCase(sisteStatus.name)
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

        @Test
        fun `burde sende sykmelding med hendelse til producer`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            sykmeldingHandterer.bekreftAvvistSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
            )

            sykmeldingProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
                .first()
                .sisteStatus()
                .status `should be equal to` HendelseStatus.BEKREFTET_AVVIST
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
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.BEKREFTET_AVVIST.name)
                .shouldContainIgnoringCase(sisteStatus.name)
        }
    }
}
