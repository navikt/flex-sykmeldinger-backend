package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.sykmelding.domain.ArbeidstakerTilleggsinfo
import no.nav.helse.flex.sykmelding.domain.SporsmalMaler
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagPasient
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TilleggsinfoSammenstillerServiceTest : FakesTestOppsett() {
    @Autowired
    lateinit var sammenstillerService: TilleggsinfoSammenstillerService

    @AfterEach
    fun afterEach() {
        slettDatabase()
    }

    @Nested
    inner class Arbeidstaker {
        @Test
        fun `burde hente riktig arbeidsgiver med narmeste leder`() {
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

            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                )
            val sporsmal =
                listOf(
                    SporsmalMaler.ARBEIDSSITUASJON.medSvar("ARBEIDSTAKER").sporsmal,
                    SporsmalMaler.ARBEIDSGIVER_ORGNUMMER.medSvar("orgnr").sporsmal,
                )

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sporsmal = sporsmal,
                    sykmelding = sykmelding,
                )

            tilleggsinfo
                .shouldNotBeNull()
                .shouldBeInstanceOf<ArbeidstakerTilleggsinfo>()
                .also {
                    it.arbeidssituasjon `should be equal to` Arbeidssituasjon.ARBEIDSTAKER
                    it.arbeidsgiver.orgnummer `should be equal to` "orgnr"
                    it.arbeidsgiver.juridiskOrgnummer `should be equal to` "jorgnr"
                    it.arbeidsgiver.orgnavn `should be equal to` "Orgnavn"
                    it.arbeidsgiver.erAktivtArbeidsforhold.shouldBeTrue()
                    it.arbeidsgiver.narmesteLeder
                        .shouldNotBeNull()
                        .navn `should be equal to` "Navn"
                }
        }

        @Test
        fun `burde akseptere at narmeste leder ikke finnes`() {
            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "fnr",
                    orgnummer = "orgnr",
                    juridiskOrgnummer = "jorgnr",
                    orgnavn = "Orgnavn",
                ),
            )

            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                )
            val sporsmal =
                listOf(
                    SporsmalMaler.ARBEIDSSITUASJON.medSvar("ARBEIDSTAKER").sporsmal,
                    SporsmalMaler.ARBEIDSGIVER_ORGNUMMER.medSvar("orgnr").sporsmal,
                )

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sporsmal = sporsmal,
                    sykmelding = sykmelding,
                )

            tilleggsinfo
                .shouldBeInstanceOf<ArbeidstakerTilleggsinfo>()
                .arbeidsgiver.narmesteLeder
                .shouldBeNull()
        }

        @Test
        fun `burder feile dersom arbeidsforhold ikke finnes`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                )

            val sporsmal =
                listOf(
                    SporsmalMaler.ARBEIDSSITUASJON.medSvar("ARBEIDSTAKER").sporsmal,
                    SporsmalMaler.ARBEIDSGIVER_ORGNUMMER.medSvar("orgnr").sporsmal,
                )

            invoking {
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sporsmal = sporsmal,
                    sykmelding = sykmelding,
                )
            } shouldThrow IllegalArgumentException::class
        }
    }
}
