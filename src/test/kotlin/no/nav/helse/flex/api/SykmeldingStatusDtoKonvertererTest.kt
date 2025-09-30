package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmeldinghendelse.*
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
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
                konvertertStatus.statusEvent `should be equal to` forventetStatusEvent
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
                konvertertArbeidsgiver `should be equal to` forventetArbeidsgiver
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
                    svar `should be equal to` JaEllerNei.JA
                    sporsmaltekst `should be equal to` "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar `should be equal to` ArbeidssituasjonDTO.ARBEIDSTAKER
                    sporsmaltekst `should be equal to` "Hvilken arbeidssituasjon?"
                }
                arbeidsgiverOrgnummer.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hva er arbeidsgiverens orgnummer?"
                    svar `should be equal to` "123456789"
                }
                riktigNarmesteLeder.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Er dette riktig nærmeste leder?"
                    svar `should be equal to` JaEllerNei.JA
                }
                harBruktEgenmeldingsdager.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du brukt egenmeldingsdager?"
                    svar `should be equal to` JaEllerNei.JA
                }
                egenmeldingsdager.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Egenmeldingsdager"
                    svar `should be equal to` listOf(dato("01.01.2025"))
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hvilke opplysninger er uriktige?"
                    svar `should be equal to` listOf(UriktigeOpplysningerType.PERIODE)
                }
                arbeidsledig.shouldBeNull()
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
                erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.NEI
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.ARBEIDSTAKER
                arbeidsgiverOrgnummer.shouldNotBeNull().svar `should be equal to` "orgnr"
                riktigNarmesteLeder.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                harBruktEgenmeldingsdager.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                egenmeldingsdager.shouldBeNull()
                uriktigeOpplysninger.shouldBeNull()
                arbeidsledig.shouldBeNull()
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
                    svar `should be equal to` JaEllerNei.JA
                    sporsmaltekst `should be equal to` "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar `should be equal to` ArbeidssituasjonDTO.ARBEIDSLEDIG
                    sporsmaltekst `should be equal to` "Hvilken arbeidssituasjon?"
                }
                arbeidsgiverOrgnummer.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hva er orgnummeret du er arbeidsledig fra?"
                    svar `should be equal to` "987654321"
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hvilke opplysninger er uriktige?"
                    svar `should be equal to` listOf(UriktigeOpplysningerType.PERIODE)
                }
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
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
                erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.NEI
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.ARBEIDSLEDIG
                arbeidsgiverOrgnummer.shouldBeNull()
                uriktigeOpplysninger.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
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
                    svar `should be equal to` JaEllerNei.JA
                    sporsmaltekst `should be equal to` "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar `should be equal to` ArbeidssituasjonDTO.PERMITTERT
                    sporsmaltekst `should be equal to` "Hvilken arbeidssituasjon?"
                }
                arbeidsgiverOrgnummer.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hva er orgnummeret du er permittert fra?"
                    svar `should be equal to` "123456789"
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hvilke opplysninger er uriktige?"
                    svar `should be equal to` listOf(UriktigeOpplysningerType.PERIODE)
                }
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
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
                erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.NEI
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.PERMITTERT
                arbeidsgiverOrgnummer.shouldBeNull()
                uriktigeOpplysninger.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
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
                    svar `should be equal to` JaEllerNei.JA
                    sporsmaltekst `should be equal to` "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar `should be equal to` ArbeidssituasjonDTO.FISKER
                    sporsmaltekst `should be equal to` "Hvilken arbeidssituasjon?"
                }
                arbeidsgiverOrgnummer.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hva er arbeidsgiverens orgnummer?"
                    svar `should be equal to` "123456789"
                }
                riktigNarmesteLeder.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Er dette riktig nærmeste leder?"
                    svar `should be equal to` JaEllerNei.JA
                }
                harBruktEgenmeldingsdager.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du brukt egenmeldingsdager?"
                    svar `should be equal to` JaEllerNei.JA
                }
                egenmeldingsdager.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Egenmeldingsdager"
                    svar `should be equal to` listOf(dato("01.01.2025"))
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hvilke opplysninger er uriktige?"
                    svar `should be equal to` listOf(UriktigeOpplysningerType.PERIODE)
                }
                harBruktEgenmelding.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du brukt egenmelding?"
                    svar `should be equal to` JaEllerNei.JA
                }
                egenmeldingsperioder.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Egenmeldingsperioder"
                    svar `should be equal to`
                        listOf(EgenmeldingsperiodeFormDTO(dato("01.01.2025") to dato("02.01.2025")))
                }
                harForsikring.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du forsikring?"
                    svar `should be equal to` JaEllerNei.JA
                }
                fisker.shouldNotBeNull().run {
                    blad.run {
                        sporsmaltekst `should be equal to` "Blad?"
                        svar `should be equal to` Blad.A
                    }
                    lottOgHyre.run {
                        sporsmaltekst `should be equal to` "Lott og hyre?"
                        svar `should be equal to` LottOgHyre.HYRE
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
                erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.NEI
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.FISKER
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                harBruktEgenmeldingsdager.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                egenmeldingsdager.shouldBeNull()
                harBruktEgenmelding.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                uriktigeOpplysninger.shouldBeNull()
                fisker.shouldNotBeNull().run {
                    blad.svar `should be equal to` Blad.B
                    lottOgHyre.svar `should be equal to` LottOgHyre.LOTT
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
                    svar `should be equal to` JaEllerNei.JA
                    sporsmaltekst `should be equal to` "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar `should be equal to` ArbeidssituasjonDTO.FRILANSER
                    sporsmaltekst `should be equal to` "Hvilken arbeidssituasjon?"
                }
                harBruktEgenmelding.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du brukt egenmelding?"
                    svar `should be equal to` JaEllerNei.JA
                }
                egenmeldingsperioder.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Egenmeldingsperioder"
                    svar `should be equal to` listOf(EgenmeldingsperiodeFormDTO(dato("01.01.2025") to dato("02.01.2025")))
                }
                harForsikring.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du forsikring?"
                    svar `should be equal to` JaEllerNei.JA
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hvilke opplysninger er uriktige?"
                    svar `should be equal to` listOf(UriktigeOpplysningerType.PERIODE)
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
                    harBruktEgenmelding = lagSporsmalSvar(false),
                    egenmeldingsperioder = null,
                    harForsikring = lagSporsmalSvar(false),
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.NEI
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.FRILANSER
                harBruktEgenmelding.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
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
                    svar `should be equal to` JaEllerNei.JA
                    sporsmaltekst `should be equal to` "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar `should be equal to` ArbeidssituasjonDTO.JORDBRUKER
                    sporsmaltekst `should be equal to` "Hvilken arbeidssituasjon?"
                }
                harBruktEgenmelding.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du brukt egenmelding?"
                    svar `should be equal to` JaEllerNei.JA
                }
                egenmeldingsperioder.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Egenmeldingsperioder"
                    svar `should be equal to` listOf(EgenmeldingsperiodeFormDTO(dato("01.01.2025") to dato("02.01.2025")))
                }
                harForsikring.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du forsikring?"
                    svar `should be equal to` JaEllerNei.JA
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hvilke opplysninger er uriktige?"
                    svar `should be equal to` listOf(UriktigeOpplysningerType.PERIODE)
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
                    harBruktEgenmelding = lagSporsmalSvar(false),
                    egenmeldingsperioder = null,
                    harForsikring = lagSporsmalSvar(false),
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.NEI
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.JORDBRUKER
                harBruktEgenmelding.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
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
                    svar `should be equal to` JaEllerNei.JA
                    sporsmaltekst `should be equal to` "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar `should be equal to` ArbeidssituasjonDTO.NAERINGSDRIVENDE
                    sporsmaltekst `should be equal to` "Hvilken arbeidssituasjon?"
                }
                harBruktEgenmelding.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du brukt egenmelding?"
                    svar `should be equal to` JaEllerNei.JA
                }
                egenmeldingsperioder.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Egenmeldingsperioder"
                    svar `should be equal to` listOf(EgenmeldingsperiodeFormDTO(dato("01.01.2025") to dato("02.01.2025")))
                }
                harForsikring.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Har du forsikring?"
                    svar `should be equal to` JaEllerNei.JA
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hvilke opplysninger er uriktige?"
                    svar `should be equal to` listOf(UriktigeOpplysningerType.PERIODE)
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
                    harBruktEgenmelding = lagSporsmalSvar(false),
                    egenmeldingsperioder = null,
                    harForsikring = lagSporsmalSvar(false),
                    uriktigeOpplysninger = null,
                )

            val konvertertStatus: SykmeldingSporsmalSvarDto =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmalSvar(brukerSvar)
            konvertertStatus.run {
                erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.NEI
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.NAERINGSDRIVENDE
                harBruktEgenmelding.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldNotBeNull().svar `should be equal to` JaEllerNei.NEI
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
                    svar `should be equal to` JaEllerNei.JA
                    sporsmaltekst `should be equal to` "Er opplysningene riktige?"
                }
                arbeidssituasjon.run {
                    svar `should be equal to` ArbeidssituasjonDTO.ANNET
                    sporsmaltekst `should be equal to` "Hvilken arbeidssituasjon?"
                }
                uriktigeOpplysninger.shouldNotBeNull().run {
                    sporsmaltekst `should be equal to` "Hvilke opplysninger er uriktige?"
                    svar `should be equal to` listOf(UriktigeOpplysningerType.PERIODE)
                }
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
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
                erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.NEI
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.ANNET
                uriktigeOpplysninger.shouldBeNull()
                arbeidsgiverOrgnummer.shouldBeNull()
                riktigNarmesteLeder.shouldBeNull()
                harBruktEgenmeldingsdager.shouldBeNull()
                egenmeldingsdager.shouldBeNull()
                harBruktEgenmelding.shouldBeNull()
                egenmeldingsperioder.shouldBeNull()
                harForsikring.shouldBeNull()
                arbeidsledig.shouldBeNull()
                fisker.shouldBeNull()
            }
        }
    }
}
