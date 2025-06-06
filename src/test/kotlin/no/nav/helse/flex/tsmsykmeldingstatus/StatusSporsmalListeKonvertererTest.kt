package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.api.dto.EgenmeldingsperiodeFormDTO
import no.nav.helse.flex.api.dto.JaEllerNei
import no.nav.helse.flex.tsmsykmeldingstatus.dto.*
import org.amshove.kluent.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.time.LocalDate

class StatusSporsmalListeKonvertererTest {
    @TestFactory
    fun `burde produsere arbeidssituasjon ANNET dersom ingen spørsmål og status SENDT eller BEKREFTET`() =
        listOf(
            StatusEventKafkaDTO.SENDT,
            StatusEventKafkaDTO.BEKREFTET,
        ).map {
            DynamicTest.dynamicTest("Status: $it") {
                StatusSporsmalListeKonverterer
                    .konverterSporsmalTilBrukerSvar(emptyList(), statusEvent = it)
                    .shouldNotBeNull()
                    .run {
                        erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
                        arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.ANNET
                    }
            }
        }

    @TestFactory
    fun `burde returnere null dersom ingen spørsmål og ikke status SENDT eller BEKREFTET`() =
        listOf(
            StatusEventKafkaDTO.APEN,
            StatusEventKafkaDTO.AVBRUTT,
            StatusEventKafkaDTO.UTGATT,
            StatusEventKafkaDTO.SLETTET,
        ).map {
            DynamicTest.dynamicTest("Status: $it") {
                StatusSporsmalListeKonverterer
                    .konverterSporsmalTilBrukerSvar(emptyList(), statusEvent = it)
                    .shouldBeNull()
            }
        }

    @Test
    fun `burde konvertere ARBEIDSTAKER med alle sporsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svar = "ARBEIDSTAKER",
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    tekst = "Jeg er sykmeldt som",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.NY_NARMESTE_LEDER,
                    svar = "NEI",
                    svartype = SvartypeKafkaDTO.JA_NEI,
                    tekst = "Er det Navn Navnesen som skal følge deg opp på jobben mens du er syk?",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.EGENMELDINGSDAGER,
                    svar = "[\"2024-04-15\",\"2024-04-16\",\"2024-04-10\"]",
                    svartype = SvartypeKafkaDTO.DAGER,
                    tekst = "Velg dagene du brukte egenmelding",
                ),
            )

        val arbeidsgiver =
            ArbeidsgiverStatusKafkaDTO(
                orgnummer = "org-nr",
                orgNavn = "Org Navn",
                juridiskOrgnummer = null,
            )

        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal, arbeidsgiver = arbeidsgiver).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.run {
                sporsmaltekst shouldBeEqualTo "Jeg er sykmeldt som"
                svar `should be equal to` ArbeidssituasjonDTO.ARBEIDSTAKER
            }
            riktigNarmesteLeder.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Er det Navn Navnesen som skal følge deg opp på jobben mens du er syk?"
                svar `should be equal to` JaEllerNei.JA
            }
            egenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Velg dagene du brukte egenmelding"
                svar `should be equal to` listOf("2024-04-15", "2024-04-16", "2024-04-10").map(LocalDate::parse)
            }
            arbeidsgiverOrgnummer.shouldNotBeNull().svar `should be equal to` "org-nr"
        }
    }

    @Test
    fun `burde konvertere ARBEIDSTAKER med kun arbeidssituasjon spørsmål`() {
        val sporsmals =
            listOf(
                SporsmalKafkaDTO(
                    tekst = "Jeg er sykmeldt som",
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = "ARBEIDSTAKER",
                ),
            )
        val arbeidsgiver =
            ArbeidsgiverStatusKafkaDTO(
                orgnummer = "org-nr",
                orgNavn = "",
                juridiskOrgnummer = null,
            )
        val brukerSvar =
            StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(
                sporsmals,
                arbeidsgiver = arbeidsgiver,
            )
        brukerSvar
            .shouldNotBeNull()
            .run {
                arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.ARBEIDSTAKER
                arbeidsgiverOrgnummer.shouldNotBeNull().svar `should be equal to` "org-nr"
            }
    }

    @Test
    fun `burde konvertere NAERINGSDRIVENDE med alle spørsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    tekst = "Jeg er sykmeldt som",
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = "NAERINGSDRIVENDE",
                ),
                SporsmalKafkaDTO(
                    tekst =
                        "Vi har registrert at du ble syk 18. januar 2023. " +
                            "Brukte du egenmelding eller noen annen sykmelding før denne datoen?",
                    shortName = ShortNameKafkaDTO.FRAVAER,
                    svartype = SvartypeKafkaDTO.JA_NEI,
                    svar = "JA",
                ),
                SporsmalKafkaDTO(
                    tekst = "Hvilke dager var du borte fra jobb før 18. januar 2023?",
                    shortName = ShortNameKafkaDTO.PERIODE,
                    svartype = SvartypeKafkaDTO.PERIODER,
                    svar = "[{\"fom\":\"2023-01-14\",\"tom\":\"2022-01-15\"}]",
                ),
                SporsmalKafkaDTO(
                    tekst = "Har du forsikring som gjelder for de første 16 dagene av sykefraværet?",
                    shortName = ShortNameKafkaDTO.FORSIKRING,
                    svartype = SvartypeKafkaDTO.JA_NEI,
                    svar = "JA",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.EGENMELDINGSDAGER,
                    svar = "[\"2023-01-10\",\"2023-01-11\",\"2023-01-13\"]",
                    svartype = SvartypeKafkaDTO.DAGER,
                    tekst = "Velg dagene du brukte egenmelding",
                ),
            )

        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.run {
                sporsmaltekst shouldBeEqualTo "Jeg er sykmeldt som"
                svar `should be equal to` ArbeidssituasjonDTO.NAERINGSDRIVENDE
            }
            harBruktEgenmelding.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Vi har registrert at du ble syk 18. januar 2023. " +
                    "Brukte du egenmelding eller noen annen sykmelding før denne datoen?"
                svar `should be equal to` JaEllerNei.JA
            }
            egenmeldingsperioder.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Hvilke dager var du borte fra jobb før 18. januar 2023?"
                svar `should be equal to`
                    listOf(
                        EgenmeldingsperiodeFormDTO(
                            fom = LocalDate.parse("2023-01-14"),
                            tom = LocalDate.parse("2022-01-15"),
                        ),
                    )
            }
            harForsikring.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Har du forsikring som gjelder for de første 16 dagene av sykefraværet?"
                svar `should be equal to` JaEllerNei.JA
            }
            egenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Velg dagene du brukte egenmelding"
                svar `should be equal to` listOf("2023-01-10", "2023-01-11", "2023-01-13").map(LocalDate::parse)
            }
        }
    }

    @Test
    fun `burde konvertere NAERINGSDRIVENDE med kun arbeidssituasjon spørsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    tekst = "Jeg er sykmeldt som",
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = "NAERINGSDRIVENDE",
                ),
            )
        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.NAERINGSDRIVENDE
            harBruktEgenmelding.shouldBeNull()
            egenmeldingsperioder.shouldBeNull()
            harForsikring.shouldBeNull()
        }
    }

    @Test
    fun `burde konvertere FRILANSERE med alle spørsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svar = "FRILANSER",
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    tekst = "Jeg er sykmeldt som",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.FRAVAER,
                    svar = "JA",
                    svartype = SvartypeKafkaDTO.JA_NEI,
                    tekst =
                        "Vi har registrert at du ble syk 18. januar 2022. " +
                            "Brukte du egenmelding eller noen annen sykmelding før denne datoen?",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.PERIODE,
                    svar = "[{\"fom\":\"2022-01-17\",\"tom\":\"2022-01-17\"}]",
                    svartype = SvartypeKafkaDTO.PERIODER,
                    tekst = "Hvilke dager var du borte fra jobb før 18. januar 2022?",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.FORSIKRING,
                    svar = "JA",
                    svartype = SvartypeKafkaDTO.JA_NEI,
                    tekst = "Har du forsikring som gjelder for de første 16 dagene av sykefraværet?",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.EGENMELDINGSDAGER,
                    svar = "[\"2023-07-31\",\"2023-08-01\",\"2023-08-03\",\"2023-08-04\",\"2023-08-11\",\"2023-08-14\"]",
                    svartype = SvartypeKafkaDTO.DAGER,
                    tekst = "Velg dagene du brukte egenmelding",
                ),
            )

        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.run {
                sporsmaltekst shouldBeEqualTo "Jeg er sykmeldt som"
                svar `should be equal to` ArbeidssituasjonDTO.FRILANSER
            }
            harBruktEgenmelding.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Vi har registrert at du ble syk 18. januar 2022. " +
                    "Brukte du egenmelding eller noen annen sykmelding før denne datoen?"
                svar `should be equal to` JaEllerNei.JA
            }
            egenmeldingsperioder.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Hvilke dager var du borte fra jobb før 18. januar 2022?"
                svar `should be equal to`
                    listOf(
                        EgenmeldingsperiodeFormDTO(
                            fom = LocalDate.parse("2022-01-17"),
                            tom = LocalDate.parse("2022-01-17"),
                        ),
                    )
            }
            harForsikring.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Har du forsikring som gjelder for de første 16 dagene av sykefraværet?"
                svar `should be equal to` JaEllerNei.JA
            }
            egenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst `should be equal to` "Velg dagene du brukte egenmelding"
                svar `should be equal to`
                    listOf(
                        "2023-07-31",
                        "2023-08-01",
                        "2023-08-03",
                        "2023-08-04",
                        "2023-08-11",
                        "2023-08-14",
                    ).map(LocalDate::parse)
            }
        }
    }

    @Test
    fun `burde konvertere FRILANSERE med kun arbeidssituasjon spørsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svar = "FRILANSER",
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    tekst = "Jeg er sykmeldt som",
                ),
            )

        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.FRILANSER
            harBruktEgenmelding.shouldBeNull()
            egenmeldingsperioder.shouldBeNull()
            harForsikring.shouldBeNull()
            egenmeldingsdager.shouldBeNull()
        }
    }

    @Test
    fun `burde konvertere ARBEIDSLEDIGE med alle spørsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svar = "ARBEIDSLEDIG",
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    tekst = "Jeg er sykmeldt som",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.EGENMELDINGSDAGER,
                    svar = "[]",
                    svartype = SvartypeKafkaDTO.DAGER,
                    tekst = "Velg dagene du brukte egenmelding",
                ),
            )

        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.run {
                sporsmaltekst shouldBeEqualTo "Jeg er sykmeldt som"
                svar `should be equal to` ArbeidssituasjonDTO.ARBEIDSLEDIG
            }
            egenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Velg dagene du brukte egenmelding"
                svar `should be equal to` emptyList()
            }
        }
    }

    @Test
    fun `burde konvertere ARBEIDSLEDIGE med kun arbeidssituasjon spørsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svar = "ARBEIDSLEDIG",
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    tekst = "Jeg er sykmeldt som",
                ),
            )

        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.ARBEIDSLEDIG
            egenmeldingsdager.shouldBeNull()
        }
    }

    @Test
    fun `burde konvertere ANNET med alle spørsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svar = "ANNET",
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    tekst = "Jeg er sykmeldt som",
                ),
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.EGENMELDINGSDAGER,
                    svar = "[\"2023-07-17\",\"2023-07-18\"]",
                    svartype = SvartypeKafkaDTO.DAGER,
                    tekst = "Velg dagene du brukte egenmelding",
                ),
            )
        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.run {
                sporsmaltekst shouldBeEqualTo "Jeg er sykmeldt som"
                svar `should be equal to` ArbeidssituasjonDTO.ANNET
            }
            egenmeldingsdager.shouldNotBeNull().run {
                sporsmaltekst shouldBeEqualTo "Velg dagene du brukte egenmelding"
                svar `should be equal to` listOf("2023-07-17", "2023-07-18").map(LocalDate::parse)
            }
        }
    }

    @Test
    fun `burde konvertere ANNET med kun arbeidssituasjon spørsmål`() {
        val sporsmal =
            listOf(
                SporsmalKafkaDTO(
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svar = "ANNET",
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    tekst = "Jeg er sykmeldt som",
                ),
            )
        StatusSporsmalListeKonverterer.konverterSporsmalTilBrukerSvar(sporsmal).shouldNotBeNull().run {
            erOpplysningeneRiktige.svar `should be equal to` JaEllerNei.JA
            arbeidssituasjon.svar `should be equal to` ArbeidssituasjonDTO.ANNET
            egenmeldingsdager.shouldBeNull()
        }
    }

    @Test
    fun `konverterNyNarmesteLederTilRiktigNarmesteLeder burde bytte om svar`() {
        StatusSporsmalListeKonverterer
            .konverterNyNarmesteLederTilRiktigNarmesteLeder(
                listOf(
                    SporsmalKafkaDTO(
                        tekst = "Skal finne ny nærmeste leder",
                        shortName = ShortNameKafkaDTO.NY_NARMESTE_LEDER,
                        svartype = SvartypeKafkaDTO.JA_NEI,
                        svar = "NEI",
                    ),
                ),
            ).shouldNotBeNull()
            .run {
                svar `should be equal to` JaEllerNei.JA
            }
    }
}
