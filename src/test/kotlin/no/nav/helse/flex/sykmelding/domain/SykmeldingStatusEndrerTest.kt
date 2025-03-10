package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.UgyldigSykmeldingStatusException
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingHendelse
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
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
                .sisteStatus()
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
            } `should throw` UgyldigSykmeldingStatusException::class
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
                .sisteStatus()
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
            } `should throw` UgyldigSykmeldingStatusException::class
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
                .sisteStatus()
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
            } `should throw` UgyldigSykmeldingStatusException::class
        }
    }

    @Nested
    inner class BekreftAvvistSykmelding {
        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.INCLUDE, names = ["APEN"])
        fun `burde gå fint med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                )

            val oppdatertSykmelding = sykmeldingStatusEndrer.endreStatusTilBekreftetAvvist(sykmelding = sykmelding)

            oppdatertSykmelding
                .sisteStatus()
                .status `should be equal to` HendelseStatus.BEKREFTET_AVVIST
        }

        @ParameterizedTest
        @EnumSource(HendelseStatus::class, mode = EnumSource.Mode.EXCLUDE, names = ["APEN"])
        fun `burde feile med siste status`(sisteStatus: HendelseStatus) {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    statuser = listOf(lagSykmeldingHendelse(status = sisteStatus)),
                )

            invoking {
                sykmeldingStatusEndrer.endreStatusTilBekreftetAvvist(sykmelding = sykmelding)
            } `should throw` UgyldigSykmeldingStatusException::class
        }
    }
}
