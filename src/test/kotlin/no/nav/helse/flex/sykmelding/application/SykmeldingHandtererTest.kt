package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.producers.sykmeldingstatus.dto.StatusEventKafkaDTO
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykmeldingStatusProducerFake
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
    lateinit var sykmeldingStatusProducer: SykmeldingStatusProducerFake

    @AfterEach
    fun cleanUp() {
        slettDatabase()
        sykmeldingStatusProducer.reset()
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

            val vilkarligBrukerSvar = lagAnnetArbeidssituasjonBrukerSvar()

            sykmeldingHandterer.sendSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                brukerSvar = vilkarligBrukerSvar,
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

            val vilkarligBrukerSvar = lagAnnetArbeidssituasjonBrukerSvar()

            sykmeldingHandterer.sendSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                brukerSvar = vilkarligBrukerSvar,
                sporsmalSvar = null,
            )

            sykmeldingStatusProducer
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

            val brukerSvar = lagArbeidstakerBrukerSvar(arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"))

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarArbeidstaker(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    hendelse.brukerSvar.`should not be null`()
                }
        }

        @Test
        fun `burde sende sykmelding for arbeidsledig`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerSvar = lagArbeidsledigBrukerSvar(arbeidsledigFraOrgnummer = null)

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarArbeidsledig(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.brukerSvar.`should not be null`()
                }
        }

        @Test
        fun `burde sende sykmelding for permittert`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerSvar = lagPermittertBrukerSvar(arbeidsledigFraOrgnummer = null)

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarPermittert(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.brukerSvar.`should not be null`()
                }
        }

        @Test
        fun `burde sende sykmelding for fisker med lott`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerSvar = lagFiskerLottBrukerSvar()

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarFiskerMedLott(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.brukerSvar.`should not be null`()
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

            val brukerSvar = lagFiskerHyreBrukerSvar(arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"))

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarFiskerMedHyre(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    hendelse.brukerSvar.`should not be null`()
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

            val brukerSvar =
                lagFiskerHyreBrukerSvar(
                    lottOgHyre = lagSporsmalSvar(FiskerLottOgHyre.BEGGE),
                    arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"),
                )

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarFiskerMedLottOgHyre(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    hendelse.brukerSvar.`should not be null`()
                }
        }

        @Test
        fun `burde sende sykmelding for jordbruker`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerSvar = lagJordbrukerBrukerSvar()

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarJordbruker(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.brukerSvar.`should not be null`()
                }
        }

        @Test
        fun `burde sende sykmelding for naringsdrivende`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerSvar = lagNaringsdrivendeBrukerSvar()

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarSelvstendigNaringsdrivende(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.brukerSvar.`should not be null`()
                }
        }

        @Test
        fun `burde sende sykmelding for frilanser`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerSvar = lagFrilanserBrukerSvar()

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarFrilanser(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.brukerSvar.`should not be null`()
                }
        }

        @Test
        fun `burde sende sykmelding for annen arbeidssituasjon`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                ),
            )

            val brukerSvar = lagAnnetArbeidssituasjonBrukerSvar()

            val sykmelding =
                sykmeldingHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                    sporsmalSvar = lagSporsmalSvarAnnet(),
                )

            sykmelding
                .sisteHendelse()
                .let { hendelse ->
                    hendelse.status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    hendelse.brukerSvar.`should not be null`()
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

            sykmeldingStatusProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
                .first()
                .statusEvent `should be equal to` StatusEventKafkaDTO.AVBRUTT
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

            sykmeldingStatusProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
                .first()
                .statusEvent `should be equal to` StatusEventKafkaDTO.BEKREFTET
        }
    }
}
