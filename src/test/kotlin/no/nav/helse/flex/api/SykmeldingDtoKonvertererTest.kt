package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.domain.tsm.*
import no.nav.helse.flex.sykmelding.domain.tsm.values.*
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import no.nav.helse.flex.testdata.lagMedisinskVurdering
import no.nav.helse.flex.testdata.lagSykmelding
import no.nav.helse.flex.testdata.lagSykmeldingGrunnlag
import org.amshove.kluent.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.OffsetDateTime

class SykmeldingDtoKonvertererTest : FakesTestOppsett() {
    @Autowired
    lateinit var sykmeldingDtoKonverterer: SykmeldingDtoKonverterer

    @Autowired
    lateinit var pdlClient: PdlClientFake

    @BeforeEach
    fun setup() {
        pdlClient.reset()
    }

    @Test
    fun `burde konvertere med riktig id`() {
        val sykmelding =
            lagSykmelding(
                sykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
            )

        val dto = sykmeldingDtoKonverterer.konverter(sykmelding)
        dto.id `should be equal to` "1"
    }

    @Test
    fun `burde konvertere pasient`() {
        val pasient =
            Pasient(
                navn = Navn("fornavn", "mellomnavn", "etternavn"),
                navKontor = null,
                navnFastlege = null,
                fnr = "fnr",
                kontaktinfo = emptyList(),
            )

        val pasientDto =
            sykmeldingDtoKonverterer.konverterPasient(
                pasient = pasient,
                fom = LocalDate.parse("2025-01-01"),
            )

        pasientDto `should be equal to`
            PasientDTO(
                fnr = "fnr",
                fornavn = "fornavn",
                mellomnavn = "mellomnavn",
                etternavn = "etternavn",
                overSyttiAar = false,
            )
    }

    @Test
    fun `burde konvertere pasient som er over 70 år`() {
        pdlClient.setFoedselsdato(LocalDate.parse("1950-01-01"), "fnr")

        val pasient =
            Pasient(
                navn = Navn("fornavn", "mellomnavn", "etternavn"),
                navKontor = null,
                navnFastlege = null,
                fnr = "fnr",
                kontaktinfo = emptyList(),
            )

        val pasientDto =
            sykmeldingDtoKonverterer.konverterPasient(
                pasient = pasient,
                fom = LocalDate.parse("2025-01-01"),
            )

        pasientDto `should be equal to`
            PasientDTO(
                fnr = "fnr",
                fornavn = "fornavn",
                mellomnavn = "mellomnavn",
                etternavn = "etternavn",
                overSyttiAar = true,
            )
    }

    @Test
    fun `burde konvertere arbeidsgiver, en arbeidsgiver har ikke info`() {
        sykmeldingDtoKonverterer
            .konverterArbeidsgiver(
                arbeidsgiverInfo =
                    EnArbeidsgiver(
                        meldingTilArbeidsgiver = "_",
                        tiltakArbeidsplassen = "_",
                    ),
            ).`should be null`()
    }

    @Test
    fun `burde konvertere arbeidsgiver, flere arbeidsgiver`() {
        val arbeidsgiver =
            FlereArbeidsgivere(
                navn = "Navn",
                yrkesbetegnelse = "_",
                stillingsprosent = 50,
                meldingTilArbeidsgiver = "_",
                tiltakArbeidsplassen = "_",
            )

        val forventetArbeidsgiver =
            ArbeidsgiverDTO(
                navn = "Navn",
                stillingsprosent = 50,
            )

        sykmeldingDtoKonverterer.konverterArbeidsgiver(arbeidsgiver) `should be equal to`
            forventetArbeidsgiver
    }

    @Test
    fun `burde konvertere arbeidsgiver, ingen arbeidsgiver`() {
        sykmeldingDtoKonverterer.konverterArbeidsgiver(IngenArbeidsgiver()) `should be equal to` null
    }

    @Test
    fun `burde konvertere aktivitet til en periode`() {
        val aktivitet =
            AktivitetIkkeMulig(
                medisinskArsak =
                    MedisinskArsak(
                        beskrivelse = "",
                        arsak = MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING,
                    ),
                arbeidsrelatertArsak =
                    ArbeidsrelatertArsak(
                        beskrivelse = "",
                        arsak = ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING,
                    ),
                fom = LocalDate.parse("2021-01-01"),
                tom = LocalDate.parse("2021-01-21"),
            )

        val periode = sykmeldingDtoKonverterer.konverterSykmeldingsperiode(aktivitet)
        periode.fom `should be equal to` LocalDate.parse("2021-01-01")
        periode.tom `should be equal to` LocalDate.parse("2021-01-21")
        periode.aktivitetIkkeMulig?.let {
            it.medisinskArsak `should be equal to`
                MedisinskArsakDTO(
                    beskrivelse = "",
                    arsak = listOf(MedisinskArsakTypeDTO.AKTIVITET_FORHINDRER_BEDRING),
                )
            it.arbeidsrelatertArsak `should be equal to`
                ArbeidsrelatertArsakDTO(
                    beskrivelse = "",
                    arsak = listOf(ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING),
                )
        }
    }

    @Test
    fun `burde konvertere medisinsk vurdering`() {
        val medisinskVurdering =
            MedisinskVurdering(
                hovedDiagnose =
                    DiagnoseInfo(
                        system = DiagnoseSystem.ICD10,
                        kode = "kode",
                    ),
                biDiagnoser =
                    listOf(
                        DiagnoseInfo(
                            system = DiagnoseSystem.ICPC2,
                            kode = "bi diagnose",
                        ),
                    ),
                svangerskap = true,
                yrkesskade =
                    Yrkesskade(
                        yrkesskadeDato = LocalDate.parse("2021-01-01"),
                    ),
                skjermetForPasient = false,
                syketilfelletStartDato = LocalDate.parse("2021-01-01"),
                annenFraversArsak =
                    AnnenFraverArsak(
                        beskrivelse = "beskrivelse",
                        arsak = listOf(AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON),
                    ),
            )

        val konvertertMedisinskVurdering =
            sykmeldingDtoKonverterer.konverterMedisinskVurdering(medisinskVurdering) `should be equal to`
                MedisinskVurderingDTO(
                    hovedDiagnose =
                        DiagnoseDTO(
                            kode = "kode",
                            system = "ICD10",
                            // TODO
                            tekst = null,
                        ),
                    biDiagnoser =
                        listOf(
                            DiagnoseDTO(
                                kode = "bi diagnose",
                                system = "ICPC2",
                                // TODO
                                tekst = null,
                            ),
                        ),
                    annenFraversArsak =
                        AnnenFraversArsakDTO(
                            beskrivelse = "beskrivelse",
                            grunn = listOf(AnnenFraverGrunnDTO.GODKJENT_HELSEINSTITUSJON),
                        ),
                    svangerskap = true,
                    yrkesskade = true,
                    yrkesskadeDato = LocalDate.parse("2021-01-01"),
                )

        sykmeldingDtoKonverterer.konverterMedisinskVurdering(medisinskVurdering) `should be equal to` konvertertMedisinskVurdering
    }

    @Test
    fun `burde konvertere annen fraværsårsak`() {
        val annenFraverArsak =
            AnnenFraverArsak(
                beskrivelse = "",
                arsak = null,
            )

        val konvertertArsak = sykmeldingDtoKonverterer.konverterAnnenFraversArsak(annenFraverArsak)
        konvertertArsak?.grunn `should be equal to` emptyList()
    }

    @Test
    fun `burde konvertere prognose`() {
        val prognose =
            Prognose(
                arbeidsforEtterPeriode = true,
                hensynArbeidsplassen = "",
                arbeid =
                    ErIArbeid(
                        egetArbeidPaSikt = true,
                        annetArbeidPaSikt = false,
                        arbeidFOM = LocalDate.parse("2021-01-01"),
                        vurderingsdato = LocalDate.parse("2021-01-01"),
                    ),
            )
        val konverterePrognose = sykmeldingDtoKonverterer.konverterPrognose(prognose)
        konverterePrognose.erIkkeIArbeid.`should be null`()
        konverterePrognose.erIArbeid?.egetArbeidPaSikt `should be` true
    }

    @Test
    fun `burde konvertere bistand Nav til melding til nav`() {
        val bistandNav =
            BistandNav(
                bistandUmiddelbart = true,
                beskrivBistand = "",
            )
        val konvertertTilMelding = sykmeldingDtoKonverterer.konverterMeldingTilNAV(bistandNav)
        konvertertTilMelding.bistandUmiddelbart `should be` true
    }

    @Test
    fun `burde konvertere behandler`() {
        val behandler =
            Behandler(
                navn =
                    Navn(
                        fornavn = "behandler",
                        mellomnavn = null,
                        etternavn = "",
                    ),
                adresse = null,
                ids =
                    listOf(
                        PersonId(
                            id = "",
                            type = PersonIdType.FNR,
                        ),
                    ),
                kontaktinfo =
                    listOf(
                        Kontaktinfo(
                            type = KontaktinfoType.TLF,
                            value = "",
                        ),
                    ),
            )

        val konvertereBehandler = sykmeldingDtoKonverterer.konverterBehandler(behandler)
        konvertereBehandler.fornavn `should be equal to` "behandler"
    }

    @Test
    fun `burde konvertere tiltak arbeidsplassen`() {
        sykmeldingDtoKonverterer
            .konverterTiltakArbeidsplassen(
                EnArbeidsgiver(
                    meldingTilArbeidsgiver = "_",
                    tiltakArbeidsplassen = "tiltak",
                ),
            ).shouldBeEqualTo("tiltak")

        sykmeldingDtoKonverterer.konverterTiltakArbeidsplassen(
            FlereArbeidsgivere(
                meldingTilArbeidsgiver = "_",
                tiltakArbeidsplassen = "tiltak",
                navn = "_",
                yrkesbetegnelse = "_",
                stillingsprosent = 0,
            ),
        ) `should be equal to` "tiltak"

        sykmeldingDtoKonverterer.konverterTiltakArbeidsplassen(
            IngenArbeidsgiver(),
        ) `should be equal to` null
    }

    @Test
    fun `burde konvertere kontakt med pasient`() {
        sykmeldingDtoKonverterer.konverterKontaktMedPasient(lagSykmeldingGrunnlag().tilbakedatering!!).`should not be null`()
    }

    @Test
    fun `burde konvertere harRedusertArbeidsgiverperiode innenfor covid periode`() {
        val medisinskVurdering =
            sykmeldingDtoKonverterer
                .konverterMedisinskVurdering(
                    lagMedisinskVurdering(
                        hovedDiagnoseKode = "R991",
                        annenFraverArsak =
                            AnnenFraverArsak(
                                beskrivelse = "beskrivelse",
                                arsak = listOf(AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON),
                            ),
                    ),
                )

        sykmeldingDtoKonverterer
            .harRedusertArbeidsgiverperiode(
                hovedDiagnose = medisinskVurdering.hovedDiagnose,
                biDiagnoser = medisinskVurdering.biDiagnoser,
                sykmeldingsperioder =
                    listOf(
                        SykmeldingsperiodeDTO(
                            fom = LocalDate.parse("2021-01-01"),
                            tom = LocalDate.parse("2021-01-31"),
                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                            reisetilskudd = false,
                            gradert = null,
                            behandlingsdager = null,
                            innspillTilArbeidsgiver = null,
                            aktivitetIkkeMulig = null,
                        ),
                    ),
                annenFraversArsakDTO = medisinskVurdering.annenFraversArsak,
            ).`should be true`()
    }

    @Test
    fun `burde konvertere harRedusertArbeidsgiverperiode utenfor covid periode`() {
        val medisinskVurdering =
            sykmeldingDtoKonverterer
                .konverterMedisinskVurdering(
                    lagMedisinskVurdering(
                        hovedDiagnoseKode = "R991",
                        annenFraverArsak =
                            AnnenFraverArsak(
                                beskrivelse = "beskrivelse",
                                arsak = listOf(AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON),
                            ),
                    ),
                )

        sykmeldingDtoKonverterer
            .harRedusertArbeidsgiverperiode(
                hovedDiagnose = medisinskVurdering.hovedDiagnose,
                biDiagnoser = medisinskVurdering.biDiagnoser,
                sykmeldingsperioder =
                    listOf(
                        SykmeldingsperiodeDTO(
                            fom = LocalDate.parse("2024-01-01"),
                            tom = LocalDate.parse("2024-01-31"),
                            type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                            reisetilskudd = false,
                            gradert = null,
                            behandlingsdager = null,
                            innspillTilArbeidsgiver = null,
                            aktivitetIkkeMulig = null,
                        ),
                    ),
                annenFraversArsakDTO = medisinskVurdering.annenFraversArsak,
            ).`should be false`()
    }

    @Test
    fun `burde konvertere behandlingsutfall`() {
        val validationResult =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                rules =
                    listOf(
                        OKRule(
                            name = "name",
                            description = "description",
                            timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                        ),
                    ),
            )

        val konvertertBehandlingsutfall =
            sykmeldingDtoKonverterer.konverterBehandlingsutfall(validationResult)
        konvertertBehandlingsutfall `should be equal to`
            BehandlingsutfallDTO(
                status = RegelStatusDTO.OK,
                ruleHits =
                    listOf(
                        RegelinfoDTO(
                            messageForSender = "",
                            messageForUser = "description",
                            ruleName = "name",
                            ruleStatus = RegelStatusDTO.OK,
                        ),
                    ),
            )
    }

    @Nested
    inner class UtdypendeOpplysningerTest {
        @Test
        fun `burde håndtere én opplysning`() {
            sykmeldingDtoKonverterer.konverterUtdypendeOpplysninger(
                mapOf(
                    "a" to
                        mapOf(
                            "b" to
                                SporsmalSvar(
                                    sporsmal = "spørsmål",
                                    svar = "svar",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                ),
            ) `should be equal to`
                mapOf(
                    "a" to
                        mapOf(
                            "b" to
                                SporsmalSvarDTO(
                                    sporsmal = "spørsmål",
                                    svar = "svar",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                )
        }

        @Test
        fun `burde håndtere restriksjoner`() {
            sykmeldingDtoKonverterer.konverterUtdypendeOpplysninger(
                mapOf(
                    "a" to
                        mapOf(
                            "b" to
                                SporsmalSvar(
                                    sporsmal = "",
                                    svar = "",
                                    restriksjoner =
                                        listOf(
                                            SvarRestriksjon.SKJERMET_FOR_NAV,
                                            SvarRestriksjon.SKJERMET_FOR_PASIENT,
                                            SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER,
                                        ),
                                ),
                        ),
                ),
            ) `should be equal to`
                mapOf(
                    "a" to
                        mapOf(
                            "b" to
                                SporsmalSvarDTO(
                                    sporsmal = "",
                                    svar = "",
                                    restriksjoner =
                                        listOf(
                                            SvarRestriksjonDTO.SKJERMET_FOR_NAV,
                                            SvarRestriksjonDTO.SKJERMET_FOR_PASIENT,
                                            SvarRestriksjonDTO.SKJERMET_FOR_ARBEIDSGIVER,
                                        ),
                                ),
                        ),
                )
        }

        @Test
        fun `burde håndtere ingen opplysning`() {
            sykmeldingDtoKonverterer.konverterUtdypendeOpplysninger(null) `should be equal to` emptyMap()
            sykmeldingDtoKonverterer.konverterUtdypendeOpplysninger(emptyMap()) `should be equal to` emptyMap()
        }

        @Test
        fun `burde håndtere flere opplysningskategorier`() {
            sykmeldingDtoKonverterer.konverterUtdypendeOpplysninger(
                mapOf(
                    "a" to
                        mapOf(
                            "a1" to
                                SporsmalSvar(
                                    sporsmal = "spørsmål a1",
                                    svar = "svar a1",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                    "b" to
                        mapOf(
                            "b1" to
                                SporsmalSvar(
                                    sporsmal = "spørsmål b1",
                                    svar = "svar b1",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                ),
            ) `should be equal to`
                mapOf(
                    "a" to
                        mapOf(
                            "a1" to
                                SporsmalSvarDTO(
                                    sporsmal = "spørsmål a1",
                                    svar = "svar a1",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                    "b" to
                        mapOf(
                            "b1" to
                                SporsmalSvarDTO(
                                    sporsmal = "spørsmål b1",
                                    svar = "svar b1",
                                    restriksjoner = emptyList(),
                                ),
                        ),
                )
        }
    }
}
