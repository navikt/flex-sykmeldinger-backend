package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykmeldingProducerFake
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
    inner class SendSykmelding {
        @Test
        fun `burde lagre hendelse`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val vilkarligBrukerInfo = AnnetArbeidssituasjonBrukerInfo

            sykmeldingHandterer.sendSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                arbeidssituasjonBrukerInfo = vilkarligBrukerInfo,
                sporsmalSvar = null,
            )

            sykmeldingRepository
                .findBySykmeldingId("1")
                .shouldNotBeNull()
                .also { it.hendelser shouldHaveSize 2 }
        }

        @Test
        fun `burde sende sykmelding med hendelse til producer`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val vilkarligBrukerInfo = AnnetArbeidssituasjonBrukerInfo

            sykmeldingHandterer.sendSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                arbeidssituasjonBrukerInfo = vilkarligBrukerInfo,
                sporsmalSvar = null,
            )

            sykmeldingProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
        }

        @Test
        fun `burde sende sykmelding for arbeidstaker`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            val brukerInfo = ArbeidstakerBrukerInfo(arbeidsgiverOrgnummer = "orgnr")

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarArbeidstaker(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 6
                }
        }

        @Test
        fun `burde sende sykmelding for arbeidsledig`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerInfo = ArbeidsledigBrukerInfo(arbeidsledigFraOrgnummer = "orgnr")

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarArbeidsledig(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 3
                }
        }

        @Test
        fun `burde sende sykmelding for permittert`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerInfo = PermittertBrukerInfo(arbeidsledigFraOrgnummer = "orgnr")

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarPermittert(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 3
                }
        }

        @Test
        fun `burde sende sykmelding for fisker med lott`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerInfo = FiskerBrukerInfo(lottOgHyre = FiskerLottOgHyre.LOTT)

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarFiskerMedLott(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 6
                    hendelse.sporsmalSvar
                        ?.find { it.tag == SporsmalTag.FISKER }
                        ?.undersporsmal
                        ?.size `should be equal to` 2
                }
        }

        @Test
        fun `burde sende sykmelding for fisker med hyre`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            val brukerInfo = FiskerBrukerInfo(lottOgHyre = FiskerLottOgHyre.HYRE, arbeidsgiverOrgnummer = "orgnr")

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarFiskerMedHyre(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 7
                    hendelse.sporsmalSvar
                        ?.find { it.tag == SporsmalTag.FISKER }
                        ?.undersporsmal
                        ?.size `should be equal to` 2
                }
        }

        @Test
        fun `burde sende sykmelding for fisker med både lott og hyre`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            arbeidsforholdRepository.save(lagArbeidsforhold(fnr = "fnr", orgnummer = "orgnr"))

            val brukerInfo = FiskerBrukerInfo(lottOgHyre = FiskerLottOgHyre.BEGGE, arbeidsgiverOrgnummer = "orgnr")

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarFiskerMedLottOgHyre(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 7
                    hendelse.sporsmalSvar
                        ?.find { it.tag == SporsmalTag.FISKER }
                        ?.undersporsmal
                        ?.size `should be equal to` 2
                }
        }

        @Test
        fun `burde sende sykmelding for jordbruker`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerInfo = JordbrukerBrukerInfo

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarJordbruker(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 5
                }
        }

        @Test
        fun `burde sende sykmelding for naringsdrivende`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerInfo = NaringsdrivendeBrukerInfo

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarSelvstendigNaringsdrivende(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 5
                }
        }

        @Test
        fun `burde sende sykmelding for frilanser`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerInfo = FrilanserBrukerInfo

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarFrilanser(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 5
                }
        }

        @Test
        fun `burde sende sykmelding for annen arbeidssituasjon`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerInfo = AnnetArbeidssituasjonBrukerInfo

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    arbeidssituasjonBrukerInfo = brukerInfo,
                    sporsmalSvar = lagSporsmalSvarAnnet(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.sporsmalSvar.`should not be null`()
                    hendelse.sporsmalSvar?.size `should be equal to` 2
                }
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
                .also { it.hendelser shouldHaveSize 2 }
                .also { it.sisteHendelse().status `should be equal to` HendelseStatus.AVBRUTT }
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
                .sisteHendelse()
                .status `should be equal to` HendelseStatus.AVBRUTT
        }
    }

    @Nested
    inner class BekreftAvvistSykmelding {
        @Test
        fun `burde lagre hendelse`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    validation = lagValidation(status = RuleType.INVALID),
                ),
            )

            sykmeldingHandterer.bekreftAvvistSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
            )

            sykmeldingRepository
                .findBySykmeldingId("1")
                .shouldNotBeNull()
                .also { it.hendelser shouldHaveSize 2 }
                .also { it.sisteHendelse().status `should be equal to` HendelseStatus.BEKREFTET_AVVIST }
        }

        @Test
        fun `burde sende sykmelding med hendelse til producer`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    validation = lagValidation(status = RuleType.INVALID),
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
                .sisteHendelse()
                .status `should be equal to` HendelseStatus.BEKREFTET_AVVIST
        }
    }
}
