package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingStatusEndrerTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusEndrer: SykmeldingStatusEndrer

    @AfterEach
    fun cleanUp() {
        slettDatabase()
    }

    @Nested
    inner class EndreStatusTilSendtTilArbeidsgiver {
        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            val endretSykmelding =
                sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                    sykmelding = sykmelding,
                    identer = PersonIdenter("fnr"),
                    arbeidsgiverOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )

            endretSykmelding
                .sisteHendelse()
                .status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                    sykmelding = sykmelding,
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
        fun `burde ikke bli sendt til arbeidsgiver dersom sykmelding er avvist`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    validation = lagValidation(status = RuleType.INVALID),
                )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            invoking {
                sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                    sykmelding = sykmelding,
                    identer = PersonIdenter("fnr"),
                    arbeidsgiverOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase("avvist")
        }

        @Test
        fun `burde ikke bli sendt til arbeidsgiver dersom sykmelding er egenmeldt`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    meldingsinformasjon = lagMeldingsinformasjonEgenmeldt(),
                )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            invoking {
                sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                    sykmelding = sykmelding,
                    identer = PersonIdenter("fnr"),
                    arbeidsgiverOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase("egenmeldt")
        }

        @Test
        fun `burde legge til arbeidstakerInfo på hendelse`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                )

            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "fnr",
                    orgnummer = "orgnr",
                    juridiskOrgnummer = "jorgnr",
                    orgnavn = "Orgnavn",
                ),
            )

            narmesteLederRepository.save(
                lagNarmesteLeder(
                    brukerFnr = "fnr",
                    orgnummer = "orgnr",
                    narmesteLederNavn = "Navn",
                ),
            )

            val endretSykmelding =
                sykmeldingStatusEndrer.endreStatusTilSendtTilArbeidsgiver(
                    sykmelding = sykmelding,
                    identer = PersonIdenter("fnr"),
                    arbeidsgiverOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )

            val arbeidstakerInfo = endretSykmelding.sisteHendelse().arbeidstakerInfo.shouldNotBeNull()
            arbeidstakerInfo.arbeidsgiver
                .also { it.orgnummer `should be equal to` "orgnr" }
                .also { it.juridiskOrgnummer `should be equal to` "jorgnr" }
                .also { it.orgnavn `should be equal to` "Orgnavn" }
                .also {
                    it.narmesteLeder
                        .shouldNotBeNull()
                        .navn `should be equal to` "Navn"
                }
        }
    }

    @Nested
    inner class EndreStatusTilSendtTilNav {
        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                )

            val endretSykmelding =
                sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
                    sykmelding = sykmelding,
                    identer = PersonIdenter("fnr"),
                    arbeidsledigFraOrgnummer = null,
                    sporsmalSvar = null,
                )

            endretSykmelding
                .sisteHendelse()
                .status `should be equal to` HendelseStatus.SENDT_TIL_NAV
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                )
            invoking {
                sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
                    sykmelding = sykmelding,
                    identer = PersonIdenter("fnr"),
                    arbeidsledigFraOrgnummer = "orgnr",
                    sporsmalSvar = null,
                )
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.SENDT_TIL_NAV.name)
                .shouldContainIgnoringCase(sisteStatus.name)
        }

        @Test
        fun `burde ikke bli sendt til Nav dersom sykmelding er avvist`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    validation = lagValidation(status = RuleType.INVALID),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
                    sykmelding = sykmelding,
                    identer = PersonIdenter("fnr"),
                    arbeidsledigFraOrgnummer = null,
                    sporsmalSvar = null,
                )
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.SENDT_TIL_NAV.name)
                .shouldContainIgnoringCase("avvist")
        }

        @Test
        fun `burde ikke bli sendt til Nav dersom sykmelding er egenmeldt`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    meldingsinformasjon = lagMeldingsinformasjonEgenmeldt(),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilSendtTilNav(
                    sykmelding = sykmelding,
                    identer = PersonIdenter("fnr"),
                    arbeidsledigFraOrgnummer = null,
                    sporsmalSvar = null,
                )
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.SENDT_TIL_NAV.name)
                .shouldContainIgnoringCase("egenmeldt")
        }
    }

    @Nested
    inner class EndreStatusTilAvbrutt {
        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                )

            val endretSykmelding = sykmeldingStatusEndrer.endreStatusTilAvbrutt(sykmelding = sykmelding)

            endretSykmelding
                .sisteHendelse()
                .status `should be equal to` HendelseStatus.AVBRUTT
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN", "SENDT_TIL_NAV", "AVBRUTT"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilAvbrutt(sykmelding = sykmelding)
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.AVBRUTT.name)
                .shouldContainIgnoringCase(sisteStatus.name)
        }

        @Test
        fun `burde ikke bli avbrutt dersom sykmelding er avvist`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    validation = lagValidation(status = RuleType.INVALID),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilAvbrutt(sykmelding = sykmelding)
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.AVBRUTT.name)
                .shouldContainIgnoringCase("avvist")
        }

        @Test
        fun `burde ikke bli avbrutt dersom sykmelding er egenmeldt`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    meldingsinformasjon = lagMeldingsinformasjonEgenmeldt(),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilAvbrutt(sykmelding = sykmelding)
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.AVBRUTT.name)
                .shouldContainIgnoringCase("egenmeldt")
        }
    }

    @Nested
    inner class BekreftAvvistSykmelding {
        @Test
        fun `burde kun akseptere sykmeldinger som er avvist`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    validation = lagValidation(status = RuleType.OK),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilBekreftetAvvist(sykmelding = sykmelding)
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.BEKREFTET_AVVIST.name)
                .shouldContainIgnoringCase("avvist")
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                    validation = lagValidation(status = RuleType.INVALID),
                )

            val oppdatertSykmelding = sykmeldingStatusEndrer.endreStatusTilBekreftetAvvist(sykmelding = sykmelding)

            oppdatertSykmelding
                .sisteHendelse()
                .status `should be equal to` HendelseStatus.BEKREFTET_AVVIST
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                    validation = lagValidation(status = RuleType.INVALID),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilBekreftetAvvist(sykmelding = sykmelding)
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.BEKREFTET_AVVIST.name)
                .shouldContainIgnoringCase(sisteStatus.name)
        }

        @Test
        fun `burde ikke bli bekreftet avvist dersom sykmelding er egenmeldt`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    meldingsinformasjon = lagMeldingsinformasjonEgenmeldt(),
                    validation = lagValidation(status = RuleType.INVALID),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilBekreftetAvvist(sykmelding = sykmelding)
            }.shouldThrow(UgyldigSykmeldingStatusException::class)
                .exceptionMessage
                .shouldContainIgnoringCase(HendelseStatus.BEKREFTET_AVVIST.name)
                .shouldContainIgnoringCase("egenmeldt")
        }
    }
}
