package no.nav.helse.flex.sykmeldinghendelse

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.sykmelding.tsm.RuleType
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.SykmeldingStatusKafkaProducerFake
import no.nav.helse.flex.testdata.*
import no.nav.helse.flex.tsmsykmeldingstatus.dto.StatusEventKafkaDTO
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class SykmeldingHendelseHandtererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingHendelseHandterer: SykmeldingHendelseHandterer

    @Autowired
    lateinit var sykmeldingStatusProducer: SykmeldingStatusKafkaProducerFake

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

            sykmeldingHendelseHandterer.sendSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                brukerSvar = vilkarligBrukerSvar,
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

            sykmeldingHendelseHandterer.sendSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
                brukerSvar = vilkarligBrukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
                )

            sykmelding
                .sisteHendelse()
                .run {
                    status `should be equal to` HendelseStatus.SENDT_TIL_NAV
                    brukerSvar.`should not be null`()
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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
                sykmeldingHendelseHandterer.sendSykmelding(
                    sykmeldingId = "1",
                    identer = PersonIdenter("fnr"),
                    brukerSvar = brukerSvar,
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

            sykmeldingHendelseHandterer.avbrytSykmelding(
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

            sykmeldingHendelseHandterer.avbrytSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
            )

            sykmeldingStatusProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
                .first()
                .event.statusEvent `should be equal to` StatusEventKafkaDTO.AVBRUTT
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

            sykmeldingHendelseHandterer.bekreftAvvistSykmelding(
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

            sykmeldingHendelseHandterer.bekreftAvvistSykmelding(
                sykmeldingId = "1",
                identer = PersonIdenter("fnr"),
            )

            sykmeldingStatusProducer
                .sendteSykmeldinger()
                .shouldHaveSize(1)
                .first()
                .event.statusEvent `should be equal to` StatusEventKafkaDTO.BEKREFTET
        }
    }
}
