package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testdata.lagSykmeldingHendelse
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class SykmeldingStatusDtoKonvertererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingStatusDtoKonverterer: SykmeldingStatusDtoKonverterer

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

    @Test
    fun `burde konvertere liste med spørsmål til gammelt format`() {
        val sporsmalOgSvarListe =
            listOf(
                Sporsmal(
                    tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
                    sporsmalstekst = "tekst",
                    svartype = Svartype.JA_NEI,
                    svar =
                        listOf(
                            Svar(
                                verdi = "JA",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.URIKTIGE_OPPLYSNINGER,
                    sporsmalstekst = "Hvilke opplysninger er uriktige?",
                    svartype = Svartype.RADIO,
                    svar =
                        listOf(
                            Svar(
                                verdi = "PERIODE",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSSITUASJON,
                    sporsmalstekst = "tekst",
                    svartype = Svartype.JA_NEI,
                    svar =
                        listOf(
                            Svar(
                                verdi = "ARBEIDSTAKER",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
                    sporsmalstekst = "Hva er arbeidsgiverens orgnummer?",
                    svartype = Svartype.FRITEKST,
                    svar =
                        listOf(
                            Svar(
                                verdi = "123456789",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSLEDIG_FRA_ORGNUMMER,
                    sporsmalstekst = "Arbeidsledig fra orgnummer?",
                    svartype = Svartype.FRITEKST,
                    svar =
                        listOf(
                            Svar(
                                verdi = "123456789",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.RIKTIG_NARMESTE_LEDER,
                    sporsmalstekst = "Er dette riktig nærmeste leder?",
                    svartype = Svartype.JA_NEI,
                    svar =
                        listOf(
                            Svar(
                                verdi = "NEI",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
                    sporsmalstekst = "Har du brukt egenmelding?",
                    svartype = Svartype.JA_NEI,
                    svar =
                        listOf(
                            Svar(
                                verdi = "NEI",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.EGENMELDINGSPERIODER,
                    sporsmalstekst = "Egenmeldingsperioder",
                    svartype = Svartype.PERIODER,
                    svar =
                        listOf(
                            Svar(
                                verdi = """{"fom": "2021-01-01", "tom": "2021-01-05"}""",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.HAR_FORSIKRING,
                    sporsmalstekst = "Har du forsikring?",
                    svartype = Svartype.JA_NEI,
                    svar =
                        listOf(
                            Svar(
                                verdi = "NEI",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.EGENMELDINGSDAGER,
                    sporsmalstekst = "Egenmeldingsdager",
                    svartype = Svartype.DATOER,
                    svar =
                        listOf(
                            Svar(
                                verdi = "2021-01-03",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDINGSDAGER,
                    sporsmalstekst = "Har du brukt egenmeldingsdager?",
                    svartype = Svartype.JA_NEI,
                    svar =
                        listOf(
                            Svar(
                                verdi = "NEI",
                            ),
                        ),
                ),
                Sporsmal(
                    tag = SporsmalTag.FISKER,
                    sporsmalstekst = "Er du fisker?",
                    svartype = Svartype.GRUPPE_AV_UNDERSPORSMAL,
                    undersporsmal =
                        listOf(
                            Sporsmal(
                                tag = SporsmalTag.FISKER__BLAD,
                                sporsmalstekst = "Hvilket blad?",
                                svartype = Svartype.RADIO,
                                svar =
                                    listOf(
                                        Svar(
                                            verdi = "A",
                                        ),
                                    ),
                            ),
                            Sporsmal(
                                tag = SporsmalTag.FISKER__LOTT_OG_HYRE,
                                sporsmalstekst = "Lott og hyre?",
                                svartype = Svartype.RADIO,
                                svar =
                                    listOf(
                                        Svar(
                                            verdi = "LOTT",
                                        ),
                                    ),
                            ),
                        ),
                ),
            )

        val forventetSporsmalSvarDto =
            SykmeldingSporsmalSvarDto(
                erOpplysningeneRiktige =
                    FormSporsmalSvar(
                        sporsmaltekst = "tekst",
                        svar = JaEllerNei.JA,
                    ),
                arbeidssituasjon =
                    FormSporsmalSvar(
                        sporsmaltekst = "tekst",
                        svar = ArbeidssituasjonDTO.ARBEIDSTAKER,
                    ),
                uriktigeOpplysninger =
                    FormSporsmalSvar(
                        sporsmaltekst = "Hvilke opplysninger er uriktige?",
                        svar = listOf(UriktigeOpplysningerType.PERIODE),
                    ),
                arbeidsgiverOrgnummer =
                    FormSporsmalSvar(
                        sporsmaltekst = "Hva er arbeidsgiverens orgnummer?",
                        svar = "123456789",
                    ),
                arbeidsledig =
                    ArbeidsledigFraOrgnummer(
                        arbeidsledigFraOrgnummer =
                            FormSporsmalSvar(
                                sporsmaltekst = "Arbeidsledig fra orgnummer?",
                                svar = "123456789",
                            ),
                    ),
                riktigNarmesteLeder =
                    FormSporsmalSvar(
                        sporsmaltekst = "Er dette riktig nærmeste leder?",
                        svar = JaEllerNei.NEI,
                    ),
                harBruktEgenmelding =
                    FormSporsmalSvar(
                        sporsmaltekst = "Har du brukt egenmelding?",
                        svar = JaEllerNei.NEI,
                    ),
                egenmeldingsperioder =
                    FormSporsmalSvar(
                        sporsmaltekst = "Egenmeldingsperioder",
                        svar =
                            listOf(
                                Egenmeldingsperiode(
                                    fom =
                                        LocalDate.parse(
                                            "2021-01-01",
                                        ),
                                    tom = LocalDate.parse("2021-01-05"),
                                ),
                            ),
                    ),
                harForsikring =
                    FormSporsmalSvar(
                        sporsmaltekst = "Har du forsikring?",
                        svar = JaEllerNei.NEI,
                    ),
                egenmeldingsdager =
                    FormSporsmalSvar(
                        sporsmaltekst = "Egenmeldingsdager",
                        svar = listOf(LocalDate.parse("2021-01-03")),
                    ),
                harBruktEgenmeldingsdager =
                    FormSporsmalSvar(
                        sporsmaltekst = "Har du brukt egenmeldingsdager?",
                        svar = JaEllerNei.NEI,
                    ),
                fisker =
                    FiskerSvar(
                        blad =
                            FormSporsmalSvar(
                                sporsmaltekst = "Hvilket blad?",
                                svar = Blad.A,
                            ),
                        lottOgHyre =
                            FormSporsmalSvar(
                                sporsmaltekst = "Lott og hyre?",
                                svar = LottOgHyre.LOTT,
                            ),
                    ),
            )

        val konvertertStatus = sykmeldingStatusDtoKonverterer.konverterSykmeldingSporsmal(sporsmalOgSvarListe)
        konvertertStatus.erOpplysningeneRiktige `should be equal to` forventetSporsmalSvarDto.erOpplysningeneRiktige
        konvertertStatus.arbeidssituasjon `should be equal to` forventetSporsmalSvarDto.arbeidssituasjon
        konvertertStatus.uriktigeOpplysninger `should be equal to` forventetSporsmalSvarDto.uriktigeOpplysninger
        konvertertStatus.arbeidsgiverOrgnummer `should be equal to` forventetSporsmalSvarDto.arbeidsgiverOrgnummer
        konvertertStatus.arbeidsledig `should be equal to` forventetSporsmalSvarDto.arbeidsledig
        konvertertStatus.riktigNarmesteLeder `should be equal to` forventetSporsmalSvarDto.riktigNarmesteLeder
        konvertertStatus.harBruktEgenmelding `should be equal to` forventetSporsmalSvarDto.harBruktEgenmelding
        konvertertStatus.egenmeldingsperioder `should be equal to` forventetSporsmalSvarDto.egenmeldingsperioder
        konvertertStatus.harForsikring `should be equal to` forventetSporsmalSvarDto.harForsikring
        konvertertStatus.egenmeldingsdager `should be equal to` forventetSporsmalSvarDto.egenmeldingsdager
        konvertertStatus.harBruktEgenmeldingsdager `should be equal to` forventetSporsmalSvarDto.harBruktEgenmeldingsdager
        konvertertStatus.fisker `should be equal to` forventetSporsmalSvarDto.fisker
    }
}
