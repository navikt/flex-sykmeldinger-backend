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
        TODO()
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
        TODO()
    }

    @Test
    fun `burde konvertere FRILANSERE med kun arbeidssituasjon spørsmål`() {
        TODO()
    }

    @Test
    fun `burde konvertere ARBEIDSLEDIGE med alle spørsmål`() {
        TODO()
    }

    @Test
    fun `burde konvertere ARBEIDSLEDIGE med kun arbeidssituasjon spørsmål`() {
        TODO()
    }

    @Test
    fun `burde konvertere ANNET med alle spørsmål`() {
        TODO()
    }

    @Test
    fun `burde konvertere ANNET med kun arbeidssituasjon spørsmål`() {
        TODO()
    }
}
