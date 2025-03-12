package no.nav.helse.flex.producers.sykmeldingstatus

import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.producers.sykmeldingstatus.dto.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import no.nav.helse.flex.testdata.lagSykmeldingHendelse
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNull
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class SykmeldingStatusKafkaDTOKonvertererTest {
    val konverterer = SykmeldingStatusKafkaDTOKonverterer()

    @Test
    fun `burde ha riktig sykmeldingId`() {
        val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"))
        val dto = konverterer.konverter(sykmelding)
        dto.sykmeldingId `should be equal to` "1"
    }

    @Test
    fun `burde ha riktig timestamp`() {
        val sykmelding =
            lagSykmelding().leggTilStatus(
                lagSykmeldingHendelse(opprettet = Instant.parse("2021-01-01T00:00:00Z")),
            )
        val dto = konverterer.konverter(sykmelding)

        dto.timestamp `should be equal to` Instant.parse("2021-01-01T00:00:00Z").tilNorgeOffsetDateTime()
    }

    @Test
    fun `burde ha riktig statusEvent`() {
        for ((sykmeldingStatus, expectedDtoStatusEvent) in listOf(
            HendelseStatus.APEN to "APEN",
            HendelseStatus.AVBRUTT to "AVBRUTT",
            HendelseStatus.SENDT_TIL_NAV to "BEKREFTET",
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER to "SENDT",
            HendelseStatus.BEKREFTET_AVVIST to "BEKREFTET",
            HendelseStatus.UTGATT to "UTGATT",
        )) {
            val sykmelding = lagSykmelding().leggTilStatus(SykmeldingHendelse(status = sykmeldingStatus, opprettet = Instant.now()))
            val dto = konverterer.konverter(sykmelding)

            dto.statusEvent `should be equal to` expectedDtoStatusEvent
        }
    }

    @Nested
    inner class BrukerSvar {
        @Test
        fun `burde feile dersom erSvareneRiktig eller arbeidssituasjon ikke er tilstede`() {
            val sporsmal = emptyList<Sporsmal>()
            val sykmelding = lagSykmelding().leggTilStatus(lagSykmeldingHendelse(sporsmalSvar = sporsmal))
            invoking {
                konverterer.konverter(sykmelding)
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

            val sykmelding = lagSykmelding().leggTilStatus(lagSykmeldingHendelse(sporsmalSvar = sporsmal))
            val dto = konverterer.konverter(sykmelding)
            dto.brukerSvar
                .shouldNotBeNull()
                .`should be equal to`(
                    BrukerSvarKafkaDTO(
                        erOpplysningeneRiktige =
                            SporsmalSvarKafkaDTO(
                                sporsmaltekst = "Er opplysningene riktige?",
                                svar = JaEllerNeiKafkaDTO.JA,
                            ),
                        uriktigeOpplysninger =
                            SporsmalSvarKafkaDTO(
                                sporsmaltekst = "Er det noen uriktige opplysninger?",
                                svar = listOf(UriktigeOpplysningerTypeKafkaDTO.PERIODE),
                            ),
                        arbeidssituasjon =
                            SporsmalSvarKafkaDTO(
                                sporsmaltekst = "Hva er din arbeidssituasjon?",
                                svar = ArbeidssituasjonKafkaDTO.ARBEIDSTAKER,
                            ),
                        arbeidsgiverOrgnummer =
                            SporsmalSvarKafkaDTO(
                                sporsmaltekst = "Hva er arbeidsgiverens organisasjonsnummer?",
                                svar = "123456789",
                            ),
                        riktigNarmesteLeder =
                            SporsmalSvarKafkaDTO(
                                sporsmaltekst = "Er dette riktig nærmeste leder?",
                                svar = JaEllerNeiKafkaDTO.JA,
                            ),
                        harBruktEgenmelding =
                            SporsmalSvarKafkaDTO(
                                sporsmaltekst = "Har du brukt egenmelding?",
                                svar = JaEllerNeiKafkaDTO.JA,
                            ),
                        egenmeldingsperioder =
                            SporsmalSvarKafkaDTO(
                                sporsmaltekst = "Hvilke egenmeldingsperioder har du hatt?",
                                svar =
                                    listOf(
                                        EgenmeldingsperiodeKafkaDTO(
                                            fom = LocalDate.parse("2021-01-01"),
                                            tom = LocalDate.parse("2021-01-05"),
                                        ),
                                    ),
                            ),
                        harForsikring = SporsmalSvarKafkaDTO("Har du forsikring?", JaEllerNeiKafkaDTO.JA),
                        egenmeldingsdager =
                            SporsmalSvarKafkaDTO(
                                "Hvilke egenmeldingsdager har du hatt?",
                                listOf(LocalDate.parse("2021-01-01")),
                            ),
                        harBruktEgenmeldingsdager = SporsmalSvarKafkaDTO("Har du brukt egenmeldingsdager?", JaEllerNeiKafkaDTO.JA),
                        fisker =
                            FiskereSvarKafkaDTO(
                                blad = SporsmalSvarKafkaDTO("Hvilket blad?", BladKafkaDTO.A),
                                lottOgHyre = SporsmalSvarKafkaDTO("Lott eller Hyre?", LottOgHyreKafkaDTO.LOTT),
                            ),
                    ),
                )
        }
    }

    enum class A {
        A,
    }

    @Test
    fun `enumValueOf burde kaste IllegalArgumentException ved ugjylidig verdi`() {
        invoking {
            enumValueOf<A>("B")
        }.shouldThrow(IllegalArgumentException::class)
    }
}
