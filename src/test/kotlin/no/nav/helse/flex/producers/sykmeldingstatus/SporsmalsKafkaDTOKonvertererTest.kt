package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.domain.Arbeidsgiver
import no.nav.helse.flex.sykmelding.domain.ArbeidstakerInfo
import no.nav.helse.flex.utils.objectMapper
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SporsmalsKafkaDTOKonvertererTest {
    private val konverterer = SporsmalsKafkaDTOKonverterer()

    @Test
    fun `Skal lage SporsmalOgSvarDTO for arbeidssituasjon`() {
        val sykmeldingFormResponse =
            BrukerSvarKafkaDTO(
                erOpplysningeneRiktige =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                uriktigeOpplysninger = null,
                arbeidssituasjon =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = ArbeidssituasjonKafkaDTO.FRILANSER,
                    ),
                arbeidsgiverOrgnummer = null,
                riktigNarmesteLeder = null,
                harBruktEgenmelding = null,
                egenmeldingsperioder = null,
                harForsikring = null,
                harBruktEgenmeldingsdager = null,
                egenmeldingsdager = null,
                fisker = null,
            )

        val sporsmalOgSvarListe = konverterer.konverterTilSporsmals(sykmeldingFormResponse)

        val expected =
            listOf(
                SporsmalKafkaDTO(
                    tekst = "",
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = ArbeidssituasjonKafkaDTO.FRILANSER.name,
                ),
            )

        sporsmalOgSvarListe `should be equal to` expected
    }

    @Test
    fun `Skal lage SporsmalOgSvarDTO for riktigNarmesteLeder med aktiv arbeidsgiver`() {
        val sykmeldingFormResponse =
            BrukerSvarKafkaDTO(
                erOpplysningeneRiktige =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                uriktigeOpplysninger = null,
                arbeidssituasjon =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = ArbeidssituasjonKafkaDTO.ARBEIDSTAKER,
                    ),
                arbeidsgiverOrgnummer =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = "123456789",
                    ),
                riktigNarmesteLeder =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                harBruktEgenmelding = null,
                egenmeldingsperioder = null,
                harForsikring = null,
                harBruktEgenmeldingsdager = null,
                egenmeldingsdager = null,
                fisker = null,
            )

        val arbeidstakerInfo =
            ArbeidstakerInfo(
                arbeidsgiver =
                    Arbeidsgiver(
                        orgnummer = "123456789",
                        juridiskOrgnummer = "",
                        orgnavn = "",
                        // aktivtArbeidsforhold = true,
                    ),
            )

        val sporsmalOgSvarListe = konverterer.konverterTilSporsmals(sykmeldingFormResponse, arbeidstakerInfo)

        val expected =
            listOf(
                SporsmalKafkaDTO(
                    tekst = "",
                    shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = ArbeidssituasjonKafkaDTO.ARBEIDSTAKER.name,
                ),
                SporsmalKafkaDTO(
                    tekst = "",
                    shortName = ShortNameKafkaDTO.NY_NARMESTE_LEDER,
                    svartype = SvartypeKafkaDTO.JA_NEI,
                    svar = JaEllerNeiKafkaDTO.NEI.name,
                ),
            )

        sporsmalOgSvarListe shouldBeEqualTo expected
    }

    @Test
    fun `Skal lage SporsmalOgSvarDTO for riktigNarmesteLeder med inaktiv arbeidsgiver`() {
        val sykmeldingFormResponse =
            BrukerSvarKafkaDTO(
                erOpplysningeneRiktige =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                uriktigeOpplysninger = null,
                arbeidssituasjon =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = ArbeidssituasjonKafkaDTO.ARBEIDSTAKER,
                    ),
                arbeidsgiverOrgnummer =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = "123456789",
                    ),
                riktigNarmesteLeder =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                harBruktEgenmelding = null,
                egenmeldingsperioder = null,
                harForsikring = null,
                harBruktEgenmeldingsdager = null,
                egenmeldingsdager = null,
                fisker = null,
            )

        val arbeidstakerInfo =
            ArbeidstakerInfo(
                arbeidsgiver =
                    Arbeidsgiver(
                        orgnummer = "132456789",
                        juridiskOrgnummer = "",
                        orgnavn = "",
//                    aktivtArbeidsforhold = false,
//                    naermesteLeder = null,
                    ),
            )

        val sporsmalOgSvarListe = konverterer.konverterTilSporsmals(sykmeldingFormResponse, arbeidstakerInfo = arbeidstakerInfo)

        val expected =
            listOf(
                SporsmalKafkaDTO(
                    "",
                    ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = ArbeidssituasjonKafkaDTO.ARBEIDSTAKER.name,
                ),
                SporsmalKafkaDTO(
                    "Skal finne ny nærmeste leder",
                    ShortNameKafkaDTO.NY_NARMESTE_LEDER,
                    SvartypeKafkaDTO.JA_NEI,
                    svar = JaEllerNeiKafkaDTO.NEI.name,
                ),
            )

        sporsmalOgSvarListe shouldBeEqualTo expected
    }

    @Test
    fun `Skal lage SporsmalOgSvarDTO for fravarSporsmal`() {
        val sykmeldingFormResponse =
            BrukerSvarKafkaDTO(
                erOpplysningeneRiktige =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                uriktigeOpplysninger = null,
                arbeidssituasjon =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = ArbeidssituasjonKafkaDTO.NAERINGSDRIVENDE,
                    ),
                arbeidsgiverOrgnummer = null,
                riktigNarmesteLeder = null,
                harBruktEgenmelding =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.NEI,
                    ),
                egenmeldingsperioder = null,
                harForsikring =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                harBruktEgenmeldingsdager = null,
                egenmeldingsdager = null,
                fisker = null,
            )

        val sporsmalOgSvarListe = konverterer.konverterTilSporsmals(sykmeldingFormResponse)

        val expected =
            listOf(
                SporsmalKafkaDTO(
                    "",
                    ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = ArbeidssituasjonKafkaDTO.NAERINGSDRIVENDE.name,
                ),
                SporsmalKafkaDTO(
                    "",
                    ShortNameKafkaDTO.FRAVAER,
                    SvartypeKafkaDTO.JA_NEI,
                    svar = JaEllerNeiKafkaDTO.NEI.name,
                ),
                SporsmalKafkaDTO(
                    "",
                    ShortNameKafkaDTO.FORSIKRING,
                    SvartypeKafkaDTO.JA_NEI,
                    svar = JaEllerNeiKafkaDTO.JA.name,
                ),
            )

        sporsmalOgSvarListe shouldBeEqualTo expected
    }

    @Test
    fun `Skal lage SporsmalOgSvarDTO for egenmeldingsperioder`() {
        val sykmeldingFormResponse =
            BrukerSvarKafkaDTO(
                erOpplysningeneRiktige =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                uriktigeOpplysninger = null,
                arbeidssituasjon =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = ArbeidssituasjonKafkaDTO.FRILANSER,
                    ),
                arbeidsgiverOrgnummer = null,
                riktigNarmesteLeder = null,
                harBruktEgenmelding =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar = JaEllerNeiKafkaDTO.JA,
                    ),
                egenmeldingsperioder =
                    SporsmalSvarKafkaDTO(
                        sporsmaltekst = "",
                        svar =
                            listOf(
                                EgenmeldingsperiodeKafkaDTO(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now(),
                                ),
                            ),
                    ),
                harForsikring = null,
                harBruktEgenmeldingsdager = null,
                egenmeldingsdager = null,
                fisker = null,
            )

        val sporsmalOgSvarListe = konverterer.konverterTilSporsmals(sykmeldingFormResponse)

        val expected =
            listOf(
                SporsmalKafkaDTO(
                    "",
                    ShortNameKafkaDTO.ARBEIDSSITUASJON,
                    SvartypeKafkaDTO.ARBEIDSSITUASJON,
                    svar = ArbeidssituasjonKafkaDTO.FRILANSER.name,
                ),
                SporsmalKafkaDTO(
                    "",
                    ShortNameKafkaDTO.FRAVAER,
                    SvartypeKafkaDTO.JA_NEI,
                    svar = JaEllerNeiKafkaDTO.JA.name,
                ),
                SporsmalKafkaDTO(
                    "",
                    ShortNameKafkaDTO.PERIODE,
                    SvartypeKafkaDTO.PERIODER,
                    svar =
                        objectMapper.writeValueAsString(
                            listOf(
                                EgenmeldingsperiodeKafkaDTO(
                                    fom = LocalDate.now(),
                                    tom = LocalDate.now(),
                                ),
                            ),
                        ),
                ),
            )

        sporsmalOgSvarListe shouldBeEqualTo expected
    }
}
