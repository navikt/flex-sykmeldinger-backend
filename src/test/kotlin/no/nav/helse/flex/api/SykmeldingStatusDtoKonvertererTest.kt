package no.nav.helse.flex.api

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.sykmeldinghendelse.*
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SykmeldingStatusDtoKonvertererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusDtoKonverterer: SykmeldingStatusDtoKonverterer

    companion object {
        private fun dato(dato: String) =
            mapOf(
                "01.01.2025" to LocalDate.parse("2025-01-01"),
                "02.01.2025" to LocalDate.parse("2025-01-02"),
            ).getValue(dato)
    }

    @TestFactory
    fun `burde konvertere fra hendelse status til sykmeldingstatus status event`() =
        listOf(
            HendelseStatus.APEN to "APEN",
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER to "SENDT",
            HendelseStatus.SENDT_TIL_NAV to "BEKREFTET",
            HendelseStatus.AVBRUTT to "AVBRUTT",
            HendelseStatus.UTGATT to "UTGATT",
            HendelseStatus.BEKREFTET_AVVIST to "BEKREFTET",
        ).map { (originalStatus, forventetStatusEvent) ->
            DynamicTest.dynamicTest("$originalStatus -> $forventetStatusEvent") {
                val originalHendelse =
                    lagSykmeldingHendelse(
                        status = originalStatus,
                    )

                val konvertertStatus = sykmeldingStatusDtoKonverterer.konverterSykmeldingStatus(originalHendelse)
                konvertertStatus.statusEvent shouldBe forventetStatusEvent
            }
        }

    @TestFactory
    fun `burde konvertere arbeidsgiver fra tilleggsinfo type`() =
        mapOf(
            "ARBEIDSTAKER" to (
                lagArbeidstakerTilleggsinfo(
                    arbeidsgiver =
                        lagArbeidsgiver(
                            orgnummer = "orgnr",
                            juridiskOrgnummer = "jurorgnr",
                            orgnavn = "orgnavn",
                        ),
                ) to
                    ArbeidsgiverStatusDTO(
                        orgnummer = "orgnr",
                        juridiskOrgnummer = "jurorgnr",
                        orgNavn = "orgnavn",
                    )
            ),
            "FISKER_LOTT" to (
                lagFiskerTilleggsinfo(
                    arbeidsgiver =
                        lagArbeidsgiver(
                            orgnummer = "orgnr",
                            juridiskOrgnummer = "jurorgnr",
                            orgnavn = "orgnavn",
                        ),
                ) to
                    ArbeidsgiverStatusDTO(
                        orgnummer = "orgnr",
                        juridiskOrgnummer = "jurorgnr",
                        orgNavn = "orgnavn",
                    )
            ),
            "FISKER_HYRE" to (
                lagFiskerTilleggsinfo(arbeidsgiver = null) to null
            ),
            "ANNET" to (
                lagArbeidsledigTilleggsinfo() to null
            ),
            "UTDATERT_FORMAT" to (
                lagUtdatertFormatTilleggsinfo(
                    arbeidsgiver =
                        UtdatertFormatArbeidsgiver(
                            orgnummer = "orgnr",
                            juridiskOrgnummer = "jurorgnr",
                            orgnavn = "orgnavn",
                        ),
                ) to
                    ArbeidsgiverStatusDTO(
                        orgnummer = "orgnr",
                        juridiskOrgnummer = "jurorgnr",
                        orgNavn = "orgnavn",
                    )
            ),
        ).map { (testNavn, testData) ->
            val (tilleggsinfo, forventetArbeidsgiver) = testData
            DynamicTest.dynamicTest(testNavn) {
                val konvertertArbeidsgiver = sykmeldingStatusDtoKonverterer.konverterArbeidsgiver(tilleggsinfo)
                konvertertArbeidsgiver shouldBe forventetArbeidsgiver
            }
        }

    @Test
    fun `burde ikke konvertere brukerSvar fra UtdatertFormatBrukerSvar`() {
        val originalHendelse =
            lagSykmeldingHendelse(
                status = HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
                brukerSvar = lagUtdatertFormatBrukerSvar(),
            )

        val konvertertStatus = sykmeldingStatusDtoKonverterer.konverterSykmeldingStatus(originalHendelse)
        konvertertStatus.brukerSvar.shouldBeNull()
    }

    @Nested
    inner class KonverterSykmeldingSporsmalSvar {
        @Test
        fun `Arbeidstaker med alle svar og sporsmalstekst`() {
            val brukerSvar =
                ArbeidstakerBrukerSvar(
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = true,
                        ),
                    arbeidssituasjon =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilken arbeidssituasjon?",
                            svar = Arbeidssituasjon.ARBEIDSTAKER,
                        ),
                    arbeidsgiverOrgnummer =
                        SporsmalSvar(
                            sporsmaltekst = "Hva er arbeidsgiverens orgnummer?",
                            svar = "123456789",
                        ),
                    riktigNarmesteLeder =
                        SporsmalSvar(
                            sporsmaltekst = "Er dette riktig nærmeste leder?",
                            svar = true,
                        ),
                    harEgenmeldingsdager =
                        SporsmalSvar(
                            sporsmaltekst = "Har du brukt egenmeldingsdager?",
                            svar = true,
                        ),
                    egenmeldingsdager =
                        SporsmalSvar(
                            sporsmaltekst = "Egenmeldingsdager",
                            svar = listOf(dato("01.01.2025")),
                        ),
                    uriktigeOpplysninger =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilke opplysninger er uriktige?",
                            svar = listOf(UriktigeOpplysning.PERIODE),
                        ),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.run {
                    svar shouldBe JaEllerNei.JA
                    sporsmaltekst shouldBe "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar shouldBe ArbeidssituasjonDTO.ARBEIDSTAKER
                    sporsmaltekst shouldBe "Hvilken arbeidssituasjon?"
                }
                arbeidsgiverOrgnummer.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hva er arbeidsgiverens orgnummer?"
                    svar shouldBe "123456789"
                }
                riktigNarmesteLeder.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Er dette riktig nærmeste leder?"
                    svar shouldBe JaEllerNei.JA
                }
                harBruktEgenmeldingsdager.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du brukt egenmeldingsdager?"
                    svar shouldBe JaEllerNei.JA
                }
                egenmeldingsdager.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Egenmeldingsdager"
                    svar shouldBe listOf(dato("01.01.2025"))
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hvilke opplysninger er uriktige?"
                    svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
                }
                arbeidsledig.shouldBeNull()
                sykFoerSykmeldingen.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Arbeidstaker med nei svar`() {
            val brukerSvar =
                ArbeidstakerBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.ARBEIDSTAKER),
                    arbeidsgiverOrgnummer = lagSporsmalSvar("orgnr"),
                    riktigNarmesteLeder = lagSporsmalSvar(false),
                    harEgenmeldingsdager = lagSporsmalSvar(false),
                    egenmeldingsdager = null,
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.ARBEIDSTAKER
                arbeidsgiverOrgnummer.shouldNotBeNull().svar shouldBe "orgnr"
                riktigNarmesteLeder.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                harBruktEgenmeldingsdager.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                egenmeldingsdager.shouldBeNull()
                uriktigeOpplysninger.shouldBeNull()
                arbeidsledig.shouldBeNull()
                sykFoerSykmeldingen.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Arbeidsledig med alle svar og sporsmalstekst`() {
            val brukerSvar =
                ArbeidsledigBrukerSvar(
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = true,
                        ),
                    arbeidssituasjon =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilken arbeidssituasjon?",
                            svar = Arbeidssituasjon.ARBEIDSLEDIG,
                        ),
                    arbeidsledigFraOrgnummer =
                        SporsmalSvar(
                            sporsmaltekst = "Hva er orgnummeret du er arbeidsledig fra?",
                            svar = "987654321",
                        ),
                    uriktigeOpplysninger =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilke opplysninger er uriktige?",
                            svar = listOf(UriktigeOpplysning.PERIODE),
                        ),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.run {
                    svar shouldBe JaEllerNei.JA
                    sporsmaltekst shouldBe "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar shouldBe ArbeidssituasjonDTO.ARBEIDSLEDIG
                    sporsmaltekst shouldBe "Hvilken arbeidssituasjon?"
                }
                arbeidsgiverOrgnummer.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hva er orgnummeret du er arbeidsledig fra?"
                    svar shouldBe "987654321"
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hvilke opplysninger er uriktige?"
                    svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
                }
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                sykFoerSykmeldingen.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Arbeidsledig med nei svar`() {
            val brukerSvar =
                ArbeidsledigBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.ARBEIDSLEDIG),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.ARBEIDSLEDIG
                arbeidsgiverOrgnummer.shouldBeNull()
                uriktigeOpplysninger.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                sykFoerSykmeldingen.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Permittert med alle svar og sporsmalstekst`() {
            val brukerSvar =
                PermittertBrukerSvar(
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = true,
                        ),
                    arbeidssituasjon =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilken arbeidssituasjon?",
                            svar = Arbeidssituasjon.PERMITTERT,
                        ),
                    arbeidsledigFraOrgnummer =
                        SporsmalSvar(
                            sporsmaltekst = "Hva er orgnummeret du er permittert fra?",
                            svar = "123456789",
                        ),
                    uriktigeOpplysninger =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilke opplysninger er uriktige?",
                            svar = listOf(UriktigeOpplysning.PERIODE),
                        ),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.run {
                    svar shouldBe JaEllerNei.JA
                    sporsmaltekst shouldBe "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar shouldBe ArbeidssituasjonDTO.PERMITTERT
                    sporsmaltekst shouldBe "Hvilken arbeidssituasjon?"
                }
                arbeidsgiverOrgnummer.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hva er orgnummeret du er permittert fra?"
                    svar shouldBe "123456789"
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hvilke opplysninger er uriktige?"
                    svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
                }
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                sykFoerSykmeldingen.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Permittert med nei svar`() {
            val brukerSvar =
                PermittertBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.PERMITTERT),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.PERMITTERT
                arbeidsgiverOrgnummer.shouldBeNull()
                uriktigeOpplysninger.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                sykFoerSykmeldingen.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Fisker med alle svar og sporsmalstekst`() {
            val brukerSvar =
                FiskerBrukerSvar(
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = true,
                        ),
                    arbeidssituasjon =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilken arbeidssituasjon?",
                            svar = Arbeidssituasjon.FISKER,
                        ),
                    lottOgHyre =
                        SporsmalSvar(
                            sporsmaltekst = "Lott og hyre?",
                            svar = FiskerLottOgHyre.HYRE,
                        ),
                    blad =
                        SporsmalSvar(
                            sporsmaltekst = "Blad?",
                            svar = FiskerBlad.A,
                        ),
                    arbeidsgiverOrgnummer =
                        SporsmalSvar(
                            sporsmaltekst = "Hva er arbeidsgiverens orgnummer?",
                            svar = "123456789",
                        ),
                    riktigNarmesteLeder =
                        SporsmalSvar(
                            sporsmaltekst = "Er dette riktig nærmeste leder?",
                            svar = true,
                        ),
                    harEgenmeldingsdager =
                        SporsmalSvar(
                            sporsmaltekst = "Har du brukt egenmeldingsdager?",
                            svar = true,
                        ),
                    egenmeldingsdager =
                        SporsmalSvar(
                            sporsmaltekst = "Egenmeldingsdager",
                            svar = listOf(dato("01.01.2025")),
                        ),
                    sykFoerSykmeldingen =
                        SporsmalSvar(
                            sporsmaltekst = "Var du syk før sykmeldingen?",
                            svar = true,
                        ),
                    harBruktEgenmelding =
                        SporsmalSvar(
                            sporsmaltekst = "Har du brukt egenmelding?",
                            svar = true,
                        ),
                    egenmeldingsperioder =
                        SporsmalSvar(
                            sporsmaltekst = "Egenmeldingsperioder",
                            svar =
                                listOf(
                                    Egenmeldingsperiode(
                                        dato("01.01.2025") to dato("02.01.2025"),
                                    ),
                                ),
                        ),
                    harForsikring =
                        SporsmalSvar(
                            sporsmaltekst = "Har du forsikring?",
                            svar = true,
                        ),
                    uriktigeOpplysninger =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilke opplysninger er uriktige?",
                            svar = listOf(UriktigeOpplysning.PERIODE),
                        ),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.run {
                    svar shouldBe JaEllerNei.JA
                    sporsmaltekst shouldBe "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar shouldBe ArbeidssituasjonDTO.FISKER
                    sporsmaltekst shouldBe "Hvilken arbeidssituasjon?"
                }
                arbeidsgiverOrgnummer.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hva er arbeidsgiverens orgnummer?"
                    svar shouldBe "123456789"
                }
                riktigNarmesteLeder.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Er dette riktig nærmeste leder?"
                    svar shouldBe JaEllerNei.JA
                }
                harBruktEgenmeldingsdager.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du brukt egenmeldingsdager?"
                    svar shouldBe JaEllerNei.JA
                }
                egenmeldingsdager.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Egenmeldingsdager"
                    svar shouldBe listOf(dato("01.01.2025"))
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hvilke opplysninger er uriktige?"
                    svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
                }
                sykFoerSykmeldingen.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Var du syk før sykmeldingen?"
                    svar shouldBe JaEllerNei.JA
                }
                harBruktEgenmelding.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du brukt egenmelding?"
                    svar shouldBe JaEllerNei.JA
                }
                egenmeldingsperioder.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Egenmeldingsperioder"
                    svar shouldBe
                        listOf(EgenmeldingsperiodeFormDTO(dato("01.01.2025") to dato("02.01.2025")))
                }
                harForsikring.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du forsikring?"
                    svar shouldBe JaEllerNei.JA
                }
                fisker.shouldNotBeNull().run {
                    blad.run {
                        sporsmaltekst shouldBe "Blad?"
                        svar shouldBe Blad.A
                    }
                    lottOgHyre.run {
                        sporsmaltekst shouldBe "Lott og hyre?"
                        svar shouldBe LottOgHyre.HYRE
                    }
                }
                arbeidsledig.shouldBeNull()
            }
        }

        @Test
        fun `Fisker med nei svar`() {
            val brukerSvar =
                FiskerBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.FISKER),
                    arbeidsgiverOrgnummer = null,
                    riktigNarmesteLeder = lagSporsmalSvar(false),
                    harEgenmeldingsdager = lagSporsmalSvar(false),
                    egenmeldingsdager = null,
                    sykFoerSykmeldingen = lagSporsmalSvar(true),
                    harBruktEgenmelding = lagSporsmalSvar(false),
                    egenmeldingsperioder = null,
                    harForsikring = lagSporsmalSvar(false),
                    uriktigeOpplysninger = null,
                    blad = lagSporsmalSvar(FiskerBlad.B),
                    lottOgHyre = lagSporsmalSvar(FiskerLottOgHyre.LOTT),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.FISKER
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                harBruktEgenmeldingsdager.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                egenmeldingsdager.shouldBeNull()
                sykFoerSykmeldingen.shouldNotBeNull().svar shouldBe JaEllerNei.JA
                harBruktEgenmelding.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                uriktigeOpplysninger.shouldBeNull()
                fisker.shouldNotBeNull().run {
                    blad.svar shouldBe Blad.B
                    lottOgHyre.svar shouldBe LottOgHyre.LOTT
                }
                arbeidsledig.shouldBeNull()
            }
        }

        @Test
        fun `Frilanser med alle svar og sporsmalstekst`() {
            val brukerSvar =
                FrilanserBrukerSvar(
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = true,
                        ),
                    arbeidssituasjon =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilken arbeidssituasjon?",
                            svar = Arbeidssituasjon.FRILANSER,
                        ),
                    sykFoerSykmeldingen =
                        SporsmalSvar(
                            sporsmaltekst = "Var du syk før sykmeldingen?",
                            svar = true,
                        ),
                    harBruktEgenmelding =
                        SporsmalSvar(
                            sporsmaltekst = "Har du brukt egenmelding?",
                            svar = true,
                        ),
                    egenmeldingsperioder =
                        SporsmalSvar(
                            sporsmaltekst = "Egenmeldingsperioder",
                            svar =
                                listOf(
                                    Egenmeldingsperiode(
                                        dato("01.01.2025") to dato("02.01.2025"),
                                    ),
                                ),
                        ),
                    harForsikring =
                        SporsmalSvar(
                            sporsmaltekst = "Har du forsikring?",
                            svar = true,
                        ),
                    uriktigeOpplysninger =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilke opplysninger er uriktige?",
                            svar = listOf(UriktigeOpplysning.PERIODE),
                        ),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.run {
                    svar shouldBe JaEllerNei.JA
                    sporsmaltekst shouldBe "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar shouldBe ArbeidssituasjonDTO.FRILANSER
                    sporsmaltekst shouldBe "Hvilken arbeidssituasjon?"
                }
                sykFoerSykmeldingen.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Var du syk før sykmeldingen?"
                    svar shouldBe JaEllerNei.JA
                }
                harBruktEgenmelding.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du brukt egenmelding?"
                    svar shouldBe JaEllerNei.JA
                }
                egenmeldingsperioder.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Egenmeldingsperioder"
                    svar shouldBe listOf(EgenmeldingsperiodeFormDTO(dato("01.01.2025") to dato("02.01.2025")))
                }
                harForsikring.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du forsikring?"
                    svar shouldBe JaEllerNei.JA
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hvilke opplysninger er uriktige?"
                    svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
                }
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Frilanser med nei svar`() {
            val brukerSvar =
                FrilanserBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.FRILANSER),
                    sykFoerSykmeldingen = lagSporsmalSvar(true),
                    harBruktEgenmelding = lagSporsmalSvar(false),
                    egenmeldingsperioder = null,
                    harForsikring = lagSporsmalSvar(false),
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.FRILANSER
                sykFoerSykmeldingen.shouldNotBeNull().svar shouldBe JaEllerNei.JA
                harBruktEgenmelding.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                uriktigeOpplysninger.shouldBeNull()
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Jordbruker med alle svar og sporsmalstekst`() {
            val brukerSvar =
                JordbrukerBrukerSvar(
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = true,
                        ),
                    arbeidssituasjon =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilken arbeidssituasjon?",
                            svar = Arbeidssituasjon.JORDBRUKER,
                        ),
                    sykFoerSykmeldingen =
                        SporsmalSvar(
                            sporsmaltekst = "Var du syk før sykmeldingen?",
                            svar = true,
                        ),
                    harBruktEgenmelding =
                        SporsmalSvar(
                            sporsmaltekst = "Har du brukt egenmelding?",
                            svar = true,
                        ),
                    egenmeldingsperioder =
                        SporsmalSvar(
                            sporsmaltekst = "Egenmeldingsperioder",
                            svar =
                                listOf(
                                    Egenmeldingsperiode(
                                        dato("01.01.2025") to dato("02.01.2025"),
                                    ),
                                ),
                        ),
                    harForsikring =
                        SporsmalSvar(
                            sporsmaltekst = "Har du forsikring?",
                            svar = true,
                        ),
                    uriktigeOpplysninger =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilke opplysninger er uriktige?",
                            svar = listOf(UriktigeOpplysning.PERIODE),
                        ),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.run {
                    svar shouldBe JaEllerNei.JA
                    sporsmaltekst shouldBe "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar shouldBe ArbeidssituasjonDTO.JORDBRUKER
                    sporsmaltekst shouldBe "Hvilken arbeidssituasjon?"
                }
                sykFoerSykmeldingen.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Var du syk før sykmeldingen?"
                    svar shouldBe JaEllerNei.JA
                }
                harBruktEgenmelding.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du brukt egenmelding?"
                    svar shouldBe JaEllerNei.JA
                }
                egenmeldingsperioder.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Egenmeldingsperioder"
                    svar shouldBe listOf(EgenmeldingsperiodeFormDTO(dato("01.01.2025") to dato("02.01.2025")))
                }
                harForsikring.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du forsikring?"
                    svar shouldBe JaEllerNei.JA
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hvilke opplysninger er uriktige?"
                    svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
                }
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Jordbruker med nei svar`() {
            val brukerSvar =
                JordbrukerBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.JORDBRUKER),
                    sykFoerSykmeldingen = lagSporsmalSvar(true),
                    harBruktEgenmelding = lagSporsmalSvar(false),
                    egenmeldingsperioder = null,
                    harForsikring = lagSporsmalSvar(false),
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.JORDBRUKER
                sykFoerSykmeldingen.shouldNotBeNull().svar shouldBe JaEllerNei.JA
                harBruktEgenmelding.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                uriktigeOpplysninger.shouldBeNull()
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Naringsdrivende med alle svar og sporsmalstekst`() {
            val brukerSvar =
                NaringsdrivendeBrukerSvar(
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = true,
                        ),
                    arbeidssituasjon =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilken arbeidssituasjon?",
                            svar = Arbeidssituasjon.NAERINGSDRIVENDE,
                        ),
                    sykFoerSykmeldingen =
                        SporsmalSvar(
                            sporsmaltekst = "Var du syk før sykmeldingen?",
                            svar = true,
                        ),
                    harBruktEgenmelding =
                        SporsmalSvar(
                            sporsmaltekst = "Har du brukt egenmelding?",
                            svar = true,
                        ),
                    egenmeldingsperioder =
                        SporsmalSvar(
                            sporsmaltekst = "Egenmeldingsperioder",
                            svar =
                                listOf(
                                    Egenmeldingsperiode(
                                        dato("01.01.2025") to dato("02.01.2025"),
                                    ),
                                ),
                        ),
                    harForsikring =
                        SporsmalSvar(
                            sporsmaltekst = "Har du forsikring?",
                            svar = true,
                        ),
                    uriktigeOpplysninger =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilke opplysninger er uriktige?",
                            svar = listOf(UriktigeOpplysning.PERIODE),
                        ),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.run {
                    svar shouldBe JaEllerNei.JA
                    sporsmaltekst shouldBe "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar shouldBe ArbeidssituasjonDTO.NAERINGSDRIVENDE
                    sporsmaltekst shouldBe "Hvilken arbeidssituasjon?"
                }
                sykFoerSykmeldingen.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Var du syk før sykmeldingen?"
                    svar shouldBe JaEllerNei.JA
                }
                harBruktEgenmelding.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du brukt egenmelding?"
                    svar shouldBe JaEllerNei.JA
                }
                egenmeldingsperioder.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Egenmeldingsperioder"
                    svar shouldBe listOf(EgenmeldingsperiodeFormDTO(dato("01.01.2025") to dato("02.01.2025")))
                }
                harForsikring.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Har du forsikring?"
                    svar shouldBe JaEllerNei.JA
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hvilke opplysninger er uriktige?"
                    svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
                }
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Naringsdrivende med nei svar`() {
            val brukerSvar =
                NaringsdrivendeBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.NAERINGSDRIVENDE),
                    sykFoerSykmeldingen = lagSporsmalSvar(true),
                    harBruktEgenmelding = lagSporsmalSvar(false),
                    egenmeldingsperioder = null,
                    harForsikring = lagSporsmalSvar(false),
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.NAERINGSDRIVENDE
                sykFoerSykmeldingen.shouldNotBeNull().svar shouldBe JaEllerNei.JA
                harBruktEgenmelding.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                uriktigeOpplysninger.shouldBeNull()
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Naringsdrivende med nei svar syk før sykmeldingen`() {
            val brukerSvar =
                NaringsdrivendeBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.NAERINGSDRIVENDE),
                    sykFoerSykmeldingen = lagSporsmalSvar(false),
                    harBruktEgenmelding = null,
                    egenmeldingsperioder = null,
                    harForsikring = lagSporsmalSvar(false),
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.NAERINGSDRIVENDE
                sykFoerSykmeldingen.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar shouldBe JaEllerNei.NEI
                uriktigeOpplysninger.shouldBeNull()
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Annet arbeidssituasjon med alle svar og sporsmalstekst`() {
            val brukerSvar =
                AnnetArbeidssituasjonBrukerSvar(
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            sporsmaltekst = "Er opplysningene riktige?",
                            svar = true,
                        ),
                    arbeidssituasjon =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilken arbeidssituasjon?",
                            svar = Arbeidssituasjon.ANNET,
                        ),
                    uriktigeOpplysninger =
                        SporsmalSvar(
                            sporsmaltekst = "Hvilke opplysninger er uriktige?",
                            svar = listOf(UriktigeOpplysning.PERIODE),
                        ),
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.run {
                    svar shouldBe JaEllerNei.JA
                    sporsmaltekst shouldBe "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar shouldBe ArbeidssituasjonDTO.ANNET
                    sporsmaltekst shouldBe "Hvilken arbeidssituasjon?"
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst shouldBe "Hvilke opplysninger er uriktige?"
                    svar shouldBe listOf(UriktigeOpplysningerType.PERIODE)
                }
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                sykFoerSykmeldingen.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }

        @Test
        fun `Annet arbeidssituasjon med nei svar`() {
            val brukerSvar =
                AnnetArbeidssituasjonBrukerSvar(
                    erOpplysningeneRiktige = lagSporsmalSvar(false),
                    arbeidssituasjon = lagSporsmalSvar(Arbeidssituasjon.ANNET),
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar shouldBe JaEllerNei.NEI
                arbeidssituasjon.svar shouldBe ArbeidssituasjonDTO.ANNET
                uriktigeOpplysninger.shouldBeNull()
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                sykFoerSykmeldingen.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }
    }
}
