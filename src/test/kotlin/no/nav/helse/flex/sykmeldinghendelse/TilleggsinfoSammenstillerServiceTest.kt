package no.nav.helse.flex.sykmeldinghendelse

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContainIgnoringCase
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.helse.flex.arbeidsforhold.lagArbeidsforhold
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.narmesteleder.lagNarmesteLeder
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.*
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
                    it.type shouldBe TilleggsinfoType.ARBEIDSTAKER
                    it.arbeidsgiver.orgnummer shouldBe "orgnr"
                    it.arbeidsgiver.juridiskOrgnummer shouldBe "jorgnr"
                    it.arbeidsgiver.orgnavn shouldBe "Orgnavn"
                    it.arbeidsgiver.erAktivtArbeidsforhold.shouldBeTrue()
                    it.arbeidsgiver.narmesteLeder
                        .shouldNotBeNull()
                        .navn shouldBe "Navn"
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

            shouldThrow<KunneIkkeFinneTilleggsinfoException> {
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            }.apply {
                message.shouldNotBeNull() shouldContainIgnoringCase "arbeidsgiver"
                message.shouldNotBeNull() shouldContainIgnoringCase "sykmelding"
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
                    it.type shouldBe TilleggsinfoType.ARBEIDSLEDIG
                    it.tidligereArbeidsgiver.shouldNotBeNull()
                    it.tidligereArbeidsgiver.orgnummer shouldBe "orgnr"
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
                    it.type shouldBe TilleggsinfoType.ARBEIDSLEDIG
                    it.tidligereArbeidsgiver.shouldBeNull()
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

            shouldThrow<KunneIkkeFinneTilleggsinfoException> {
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            }
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
                    it.type shouldBe TilleggsinfoType.PERMITTERT
                    it.tidligereArbeidsgiver.shouldNotBeNull()
                    it.tidligereArbeidsgiver.orgnummer shouldBe "orgnr"
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
                    it.type shouldBe TilleggsinfoType.PERMITTERT
                    it.tidligereArbeidsgiver.shouldBeNull()
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

            shouldThrow<KunneIkkeFinneTilleggsinfoException> {
                sammenstillerService.sammenstillTilleggsinfo(
                    identer = PersonIdenter("fnr"),
                    sykmelding = sykmelding,
                    brukerSvar = brukerSvar,
                )
            }
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
                        type shouldBe TilleggsinfoType.FISKER
                        arbeidsgiver
                            .shouldNotBeNull()
                            .run {
                                orgnummer shouldBe "orgnr"
                                juridiskOrgnummer shouldBe "jorgnr"
                                orgnavn shouldBe "Orgnavn"
                                erAktivtArbeidsforhold.shouldBeTrue()
                                narmesteLeder
                                    .shouldNotBeNull()
                                    .navn shouldBe "Navn"
                            }
                    }
            }

            @ParameterizedTest
            @EnumSource(FiskerLottOgHyre::class, names = ["HYRE", "BEGGE"])
            fun `burde akseptere at arbeidsgiver ikke finnes i brukersvar`(fiskerLottOgHyre: FiskerLottOgHyre) {
                val sykmelding =
                    lagSykmelding(
                        sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1", lagPasient(fnr = "fnr")),
                    )

                val brukerSvar =
                    lagFiskerHyreBrukerSvar(
                        arbeidsgiverOrgnummer = null,
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
                        type shouldBe TilleggsinfoType.FISKER
                        arbeidsgiver
                            .shouldBeNull()
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

                shouldThrow<KunneIkkeFinneTilleggsinfoException> {
                    sammenstillerService.sammenstillTilleggsinfo(
                        identer = PersonIdenter("fnr"),
                        sykmelding = sykmelding,
                        brukerSvar = brukerSvar,
                    )
                }.apply {
                    message.shouldNotBeNull() shouldContainIgnoringCase "arbeidsgiver"
                    message.shouldNotBeNull() shouldContainIgnoringCase "sykmelding"
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
                        it.type shouldBe TilleggsinfoType.FISKER
                        it.arbeidsgiver.shouldBeNull()
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
                    it.type shouldBe TilleggsinfoType.FRILANSER
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
                    it.type shouldBe TilleggsinfoType.NAERINGSDRIVENDE
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
                    it.type shouldBe TilleggsinfoType.JORDBRUKER
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
                    it.type shouldBe TilleggsinfoType.ANNET
                }
        }
    }
}
