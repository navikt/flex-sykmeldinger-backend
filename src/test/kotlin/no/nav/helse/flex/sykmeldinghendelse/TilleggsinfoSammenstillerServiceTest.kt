package no.nav.helse.flex.sykmeldinghendelse

import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.sykmeldinghendelse.FiskerLottOgHyre
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

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
                    arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"),
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
                    it.type `should be equal to` TilleggsinfoType.ARBEIDSTAKER
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
                    arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"),
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

            val brukerSvar = lagArbeidstakerBrukerSvar(arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"))

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
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            lagPasient(fnr = "fnr"),
                            aktiviteter =
                                listOf(
                                    lagAktivitetIkkeMulig(
                                        LocalDate.parse("2021-01-01"),
                                        LocalDate.parse("2021-01-10"),
                                    ),
                                ),
                        ),
                ).leggTilHendelse(
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo = lagArbeidstakerTilleggsinfo(arbeidsgiver = lagArbeidsgiver(orgnummer = "orgnr")),
                        ),
                ),
            )

            val sykmelding =
                sykmeldingRepository.save(
                    lagSykmelding(
                        sykmeldingGrunnlag =
                            lagSykmeldingGrunnlag(
                                id = "2",
                                lagPasient(fnr = "fnr"),
                                aktiviteter =
                                    listOf(
                                        lagAktivitetIkkeMulig(
                                            LocalDate.parse("2021-01-11"),
                                            LocalDate.parse("2021-01-20"),
                                        ),
                                    ),
                            ),
                    ),
                )

            val brukerSvar =
                lagArbeidsledigBrukerSvar(
                    arbeidsledigFraOrgnummer = lagSporsmalSvar("orgnr"),
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
                    it.type `should be equal to` TilleggsinfoType.ARBEIDSLEDIG
                    it.tidligereArbeidsgiver.`should not be null`()
                    it.tidligereArbeidsgiver?.orgnummer `should be equal to` "orgnr"
                }
        }

        @Test
        fun `burde akseptere at valgt arbeidsgiver er null`() {
            val sykmelding =
                sykmeldingRepository.save(
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    ),
                )

            val brukerSvar =
                lagArbeidsledigBrukerSvar(
                    arbeidsledigFraOrgnummer = null,
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
                    it.type `should be equal to` TilleggsinfoType.ARBEIDSLEDIG
                    it.tidligereArbeidsgiver.`should be null`()
                }
        }

        @Test
        fun `burde feile dersom valgt tidligere arbeidsgiver ikke finnes`() {
            val sykmelding =
                sykmeldingRepository.save(
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", lagPasient(fnr = "fnr")),
                    ),
                )

            val brukerSvar =
                lagArbeidsledigBrukerSvar(
                    arbeidsledigFraOrgnummer = lagSporsmalSvar("orgnr"),
                )

            invoking {
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            } `should throw` KunneIkkeFinneTilleggsinfoException::class
        }
    }

    @Nested
    inner class Permittert {
        @Test
        fun `burde hente riktig tidligere arbeidsgiver`() {
            sykmeldingRepository.save(
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            id = "1",
                            lagPasient(fnr = "fnr"),
                            aktiviteter =
                                listOf(
                                    lagAktivitetIkkeMulig(
                                        LocalDate.parse("2021-01-01"),
                                        LocalDate.parse("2021-01-10"),
                                    ),
                                ),
                        ),
                ).leggTilHendelse(
                    sykmeldingHendelse =
                        lagSykmeldingHendelse(
                            status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                            tilleggsinfo = lagArbeidstakerTilleggsinfo(arbeidsgiver = lagArbeidsgiver(orgnummer = "orgnr")),
                        ),
                ),
            )

            val sykmelding =
                sykmeldingRepository.save(
                    lagSykmelding(
                        sykmeldingGrunnlag =
                            lagSykmeldingGrunnlag(
                                id = "2",
                                lagPasient(fnr = "fnr"),
                                aktiviteter =
                                    listOf(
                                        lagAktivitetIkkeMulig(
                                            LocalDate.parse("2021-01-11"),
                                            LocalDate.parse("2021-01-20"),
                                        ),
                                    ),
                            ),
                    ),
                )

            val brukerSvar =
                lagPermittertBrukerSvar(
                    arbeidsledigFraOrgnummer = lagSporsmalSvar("orgnr"),
                )

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )

            tilleggsinfo
                .shouldNotBeNull()
                .shouldBeInstanceOf<PermittertTilleggsinfo>()
                .also {
                    it.type `should be equal to` TilleggsinfoType.PERMITTERT
                    it.tidligereArbeidsgiver.`should not be null`()
                    it.tidligereArbeidsgiver?.orgnummer `should be equal to` "orgnr"
                }
        }

        @Test
        fun `burde akseptere at valgt arbeidsgiver er null`() {
            val sykmelding =
                sykmeldingRepository.save(
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    ),
                )

            val brukerSvar =
                lagPermittertBrukerSvar(
                    arbeidsledigFraOrgnummer = null,
                )

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )

            tilleggsinfo
                .shouldNotBeNull()
                .shouldBeInstanceOf<PermittertTilleggsinfo>()
                .also {
                    it.type `should be equal to` TilleggsinfoType.PERMITTERT
                    it.tidligereArbeidsgiver.`should be null`()
                }
        }

        @Test
        fun `burde feile dersom valgt tidligere arbeidsgiver ikke finnes`() {
            val sykmelding =
                sykmeldingRepository.save(
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", lagPasient(fnr = "fnr")),
                    ),
                )

            val brukerSvar =
                lagPermittertBrukerSvar(
                    arbeidsledigFraOrgnummer = lagSporsmalSvar("orgnr"),
                )

            invoking {
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            } `should throw` KunneIkkeFinneTilleggsinfoException::class
        }
    }

    @Nested
    inner class Fisker {
        @Nested
        inner class HyreOgBegge {
            @ParameterizedTest
            @EnumSource(FiskerLottOgHyre::class, names = ["HYRE", "BEGGE"])
            fun `burde hente riktig arbeidsgiver med narmeste leder`(fiskerLottOgHyre: FiskerLottOgHyre) {
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
                    lagFiskerHyreBrukerSvar(
                        arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"),
                        lottOgHyre = lagSporsmalSvar(fiskerLottOgHyre),
                    )

                val tilleggsinfo =
                    sammenstillerService.sammenstillTilleggsinfo(
                        identer = PersonIdenter("fnr"),
                        sykmelding = sykmelding,
                        brukerSvar = brukerSvar,
                    )

                tilleggsinfo
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<FiskerTilleggsinfo>()
                    .run {
                        type `should be equal to` TilleggsinfoType.FISKER
                        arbeidsgiver
                            .`should not be null`()
                            .run {
                                orgnummer `should be equal to` "orgnr"
                                juridiskOrgnummer `should be equal to` "jorgnr"
                                orgnavn `should be equal to` "Orgnavn"
                                erAktivtArbeidsforhold.`should be true`()
                                narmesteLeder
                                    .`should not be null`()
                                    .navn `should be equal to` "Navn"
                            }
                    }
            }

            @ParameterizedTest
            @EnumSource(FiskerLottOgHyre::class, names = ["HYRE", "BEGGE"])
            fun `burde akseptere at narmeste leder ikke finnes`(fiskerLottOgHyre: FiskerLottOgHyre) {
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
                    lagFiskerHyreBrukerSvar(
                        arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"),
                        lottOgHyre = lagSporsmalSvar(fiskerLottOgHyre),
                    )

                val tilleggsinfo =
                    sammenstillerService.sammenstillTilleggsinfo(
                        identer = PersonIdenter("fnr"),
                        sykmelding = sykmelding,
                        brukerSvar = brukerSvar,
                    )

                tilleggsinfo
                    .shouldBeInstanceOf<FiskerTilleggsinfo>()
                    .arbeidsgiver
                    .shouldNotBeNull()
                    .narmesteLeder
                    .shouldBeNull()
            }

            @ParameterizedTest
            @EnumSource(FiskerLottOgHyre::class, names = ["HYRE", "BEGGE"])
            fun `burde feile dersom arbeidsforhold ikke finnes`(fiskerLottOgHyre: FiskerLottOgHyre) {
                val sykmelding =
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    )

                val brukerSvar =
                    lagFiskerHyreBrukerSvar(
                        arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"),
                        lottOgHyre = lagSporsmalSvar(fiskerLottOgHyre),
                    )

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
        inner class Lott {
            @Test
            fun `burde returnere riktig tilleggsinfo`() {
                val sykmelding =
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    )

                val brukerSvar = lagFiskerLottBrukerSvar()

                val tilleggsinfo =
                    sammenstillerService.sammenstillTilleggsinfo(
                        identer = PersonIdenter("fnr"),
                        sykmelding = sykmelding,
                        brukerSvar = brukerSvar,
                    )

                tilleggsinfo
                    .shouldNotBeNull()
                    .shouldBeInstanceOf<FiskerTilleggsinfo>()
                    .also {
                        it.type `should be equal to` TilleggsinfoType.FISKER
                        it.arbeidsgiver.`should be null`()
                    }
            }
        }
    }

    @Nested
    inner class Frilanser {
        @Test
        fun `burde returnere riktig tilleggsinfo`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", lagPasient(fnr = "fnr")),
                )

            val brukerSvar =
                lagFrilanserBrukerSvar()

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )

            tilleggsinfo
                .shouldNotBeNull()
                .shouldBeInstanceOf<FrilanserTilleggsinfo>()
                .also {
                    it.type `should be equal to` TilleggsinfoType.FRILANSER
                }
        }
    }

    @Nested
    inner class Naringsdrivende {
        @Test
        fun `burde returnere riktig tilleggsinfo`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", lagPasient(fnr = "fnr")),
                )

            val brukerSvar =
                lagNaringsdrivendeBrukerSvar()

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )

            tilleggsinfo
                .shouldNotBeNull()
                .shouldBeInstanceOf<NaringsdrivendeTilleggsinfo>()
                .also {
                    it.type `should be equal to` TilleggsinfoType.NAERINGSDRIVENDE
                }
        }
    }

    @Nested
    inner class Jordbruker {
        @Test
        fun `burde returnere riktig tilleggsinfo`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", lagPasient(fnr = "fnr")),
                )

            val brukerSvar =
                lagJordbrukerBrukerSvar()

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )

            tilleggsinfo
                .shouldNotBeNull()
                .shouldBeInstanceOf<JordbrukerTilleggsinfo>()
                .also {
                    it.type `should be equal to` TilleggsinfoType.JORDBRUKER
                }
        }
    }

    @Nested
    inner class AnnetArbeidssituasjon {
        @Test
        fun `burde returnere riktig tilleggsinfo`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "2", lagPasient(fnr = "fnr")),
                )

            val brukerSvar =
                lagAnnetArbeidssituasjonBrukerSvar()

            val tilleggsinfo =
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )

            tilleggsinfo
                .shouldNotBeNull()
                .shouldBeInstanceOf<AnnetArbeidssituasjonTilleggsinfo>()
                .also {
                    it.type `should be equal to` TilleggsinfoType.ANNET
                }
        }
    }
}
