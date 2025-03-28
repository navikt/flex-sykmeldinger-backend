package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.api.dto.Blad
import no.nav.helse.flex.api.dto.FormSporsmalSvar
import no.nav.helse.flex.api.dto.LottOgHyre
import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.domain.Sporsmal
import no.nav.helse.flex.sykmelding.domain.SporsmalTag
import no.nav.helse.flex.sykmelding.domain.Svar
import no.nav.helse.flex.sykmelding.domain.Svartype
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Disabled
class BrukerSvarKafkaDTOKonvertererTest {
    private val konverterer = BrukerSvarKafkaDTOKonverterer()

    @Test
    fun `burde feile dersom erSvareneRiktig eller arbeidssituasjon ikke er tilstede`() {
        val sporsmal = emptyList<Sporsmal>()
        invoking {
            konverterer.konverterTilBrukerSvar(sporsmal)
        }.shouldThrow(Exception::class)
    }

    @Test
    fun `burde konvertere alle svar`() {
        val sporsmal =
            listOf(
                Sporsmal(
                    tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
                    sporsmalstekst = "Er opplysningene riktige?",
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = "JA")),
                ),
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSSITUASJON,
                    sporsmalstekst = "Hva er din arbeidssituasjon?",
                    svartype = Svartype.RADIO,
                    svar = listOf(Svar(verdi = "ARBEIDSTAKER")),
                ),
                Sporsmal(
                    tag = SporsmalTag.URIKTIGE_OPPLYSNINGER,
                    sporsmalstekst = "Er det noen uriktige opplysninger?",
                    svartype = Svartype.CHECKBOX,
                    svar = listOf(Svar(verdi = "PERIODE")),
                ),
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
                    sporsmalstekst = "Hva er arbeidsgiverens organisasjonsnummer?",
                    svartype = Svartype.FRITEKST,
                    svar = listOf(Svar(verdi = "123456789")),
                ),
                Sporsmal(
                    tag = SporsmalTag.RIKTIG_NARMESTE_LEDER,
                    sporsmalstekst = "Er dette riktig nærmeste leder?",
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = "JA")),
                ),
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
                    sporsmalstekst = "Har du brukt egenmelding?",
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = "JA")),
                ),
                Sporsmal(
                    tag = SporsmalTag.EGENMELDINGSPERIODER,
                    sporsmalstekst = "Hvilke egenmeldingsperioder har du hatt?",
                    svartype = Svartype.PERIODER,
                    svar =
                        listOf(
                            Svar(verdi = """{"fom": "2025-01-01", "tom": "2025-01-05"}"""),
                            Svar(verdi = """{"fom": "2025-01-10", "tom": "2025-01-15"}"""),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.HAR_FORSIKRING,
                    sporsmalstekst = "Har du forsikring?",
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = "JA")),
                ),
                Sporsmal(
                    tag = SporsmalTag.EGENMELDINGSDAGER,
                    sporsmalstekst = "Hvilke egenmeldingsdager har du hatt?",
                    svartype = Svartype.DATOER,
                    svar = listOf(Svar(verdi = "2021-01-01")),
                ),
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDINGSDAGER,
                    sporsmalstekst = "Har du brukt egenmeldingsdager?",
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = "JA")),
                ),
                Sporsmal(
                    tag = SporsmalTag.FISKER,
                    svartype = Svartype.GRUPPE_AV_UNDERSPORSMAL,
                    undersporsmal =
                        listOf(
                            Sporsmal(
                                tag = SporsmalTag.FISKER__BLAD,
                                sporsmalstekst = "Hvilket blad?",
                                svartype = Svartype.RADIO,
                                svar = listOf(Svar(verdi = "A")),
                            ),
                            Sporsmal(
                                tag = SporsmalTag.FISKER__LOTT_OG_HYRE,
                                sporsmalstekst = "Lott eller Hyre?",
                                svartype = Svartype.RADIO,
                                svar = listOf(Svar(verdi = "LOTT")),
                            ),
                        ),
                ),
            )

        val brukerSvar = konverterer.konverterTilBrukerSvar(sporsmal)

        brukerSvar.erOpplysningeneRiktige `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Er opplysningene riktige?",
                svar = JaEllerNeiKafkaDTO.JA,
            )
        brukerSvar.uriktigeOpplysninger `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Er det noen uriktige opplysninger?",
                svar = listOf(UriktigeOpplysningerTypeKafkaDTO.PERIODE),
            )

        brukerSvar.erOpplysningeneRiktige `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Er opplysningene riktige?",
                svar = JaEllerNeiKafkaDTO.JA,
            )
        brukerSvar.uriktigeOpplysninger `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Er det noen uriktige opplysninger?",
                svar = listOf(UriktigeOpplysningerTypeKafkaDTO.PERIODE),
            )
        brukerSvar.arbeidssituasjon `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Hva er din arbeidssituasjon?",
                svar = ArbeidssituasjonKafkaDTO.ARBEIDSTAKER,
            )
        brukerSvar.arbeidsgiverOrgnummer `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Hva er arbeidsgiverens organisasjonsnummer?",
                svar = "123456789",
            )
        brukerSvar.riktigNarmesteLeder `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Er dette riktig nærmeste leder?",
                svar = JaEllerNeiKafkaDTO.JA,
            )
        brukerSvar.harBruktEgenmelding `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Har du brukt egenmelding?",
                svar = JaEllerNeiKafkaDTO.JA,
            )
        brukerSvar.egenmeldingsperioder `should be equal to`
            SporsmalSvarKafkaDTO(
                sporsmaltekst = "Hvilke egenmeldingsperioder har du hatt?",
                svar =
                    listOf(
                        EgenmeldingsperiodeKafkaDTO(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-01-05"),
                        ),
                        EgenmeldingsperiodeKafkaDTO(
                            fom = LocalDate.parse("2025-01-10"),
                            tom = LocalDate.parse("2025-01-15"),
                        ),
                    ),
            )
        brukerSvar.harForsikring `should be equal to` SporsmalSvarKafkaDTO("Har du forsikring?", JaEllerNeiKafkaDTO.JA)
        brukerSvar.egenmeldingsdager `should be equal to`
            SporsmalSvarKafkaDTO(
                "Hvilke egenmeldingsdager har du hatt?",
                listOf(LocalDate.parse("2021-01-01")),
            )
        brukerSvar.harBruktEgenmeldingsdager `should be equal to`
            SporsmalSvarKafkaDTO("Har du brukt egenmeldingsdager?", JaEllerNeiKafkaDTO.JA)
        brukerSvar.fisker `should be equal to`
            FiskereSvarKafkaDTO(
                blad = FormSporsmalSvar("Hvilket blad?", Blad.A),
                lottOgHyre = FormSporsmalSvar("Lott eller Hyre?", LottOgHyre.LOTT),
            )
    }
}
