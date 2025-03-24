package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.sykmelding.domain.ArbeidsledigTilleggsinfo
import no.nav.helse.flex.sykmelding.domain.ArbeidstakerTilleggsinfo
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.*
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

            val brukerSvar =
                lagArbeidstakerBrukerSvar(
                    arbeidsgiverOrgnummer = "orgnr",
                )

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
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
            val brukerSvar =
                lagArbeidstakerBrukerSvar(
                    arbeidsgiverOrgnummer = "orgnr",
                )

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
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

            val brukerSvar = lagArbeidstakerBrukerSvar(arbeidsgiverOrgnummer = "orgnr")

            invoking {
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            }.shouldThrow(KunneIkkeFinneTilleggsinfoException::class)
                .apply {
                    exceptionMessage shouldContainIgnoringCase "arbeidsgiver"
                    exceptionMessage shouldContainIgnoringCase "sykmelding"
                }
        }
    }

    @Nested
    inner class Arbeidsledig {
        @Test
        fun `burde hente riktig tidligere arbeidsgiver`() {
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
            ).leggTilHendelse(
                sykmeldingHendelse =
                    lagSykmeldingHendelse(
                        status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                        brukerSvar = lagArbeidstakerBrukerSvar(arbeidsgiverOrgnummer = "orgnr"),
                    ),
            ).also {
                sykmeldingRepository.save(it)
            }

            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", lagPasient(fnr = "fnr")),
                )

            val brukerSvar =
                lagArbeidsledigBrukerSvar(
                    arbeidsledigFraOrgnummer = "orgnr",
                )

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )

            tilleggsinfo
                .shouldNotBeNull()
                .shouldBeInstanceOf<ArbeidsledigTilleggsinfo>()
                .also {
                    it.arbeidssituasjon `should be equal to` Arbeidssituasjon.ARBEIDSTAKER
                    it.tidligereArbeidsgiver.`should not be null`()
                    it.tidligereArbeidsgiver?.orgnummer `should be equal to` "orgnr"
                }
        }

        @Test
        fun `burde akseptere at valgt arbeidsgiver er null`() {
        }

        @Test
        fun `burde feile dersom valgt tidligere arbeidsgiver ikke finnes`() {
        }
    }
}
