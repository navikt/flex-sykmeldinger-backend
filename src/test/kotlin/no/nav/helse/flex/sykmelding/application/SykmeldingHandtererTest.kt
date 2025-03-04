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

            sykmeldingHandterer.sendSykmeldingTilArbeidsgiver(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                arbeidsgiverOrgnummer = "orgnr",
                sporsmalSvar = null,
            )

            val sykmelding = sykmeldingRepository.findBySykmeldingId("1")
            sykmelding
                .shouldNotBeNull()
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
}
