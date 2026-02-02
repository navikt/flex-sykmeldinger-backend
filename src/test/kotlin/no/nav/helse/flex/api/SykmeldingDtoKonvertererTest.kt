package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.tsm.*
import no.nav.helse.flex.sykmelding.tsm.values.*
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import no.nav.helse.flex.testdata.*
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

    @Nested
    inner class KonverteringTest {
        @Test
        fun `burde mappe felles felter for alle sykmeldinger`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(),
                    validation = lagValidation(status = RuleType.OK),
                )
            val sykmeldingsperioderDto =
                sykmelding.sykmeldingGrunnlag.aktivitet.map {
                    sykmeldingDtoKonverterer.konverterSykmeldingsperiode(it)
                }
            val medisinskVurderingDto =
                sykmeldingDtoKonverterer.konverterMedisinskVurdering(
                    sykmelding.sykmeldingGrunnlag.medisinskVurdering,
                )

            val dto = sykmeldingDtoKonverterer.konverterTilSykmeldingDTO(sykmelding)

            dto.pasient `should be equal to`
                sykmeldingDtoKonverterer.konverterPasient(
                    sykmelding.sykmeldingGrunnlag.pasient,
                    sykmeldingsperioderDto.minBy { it.fom }.fom,
                )
            dto.medisinskVurdering `should be equal to` medisinskVurderingDto
            dto.sykmeldingsperioder `should be equal to` sykmeldingsperioderDto
            dto.behandlingsutfall `should be equal to` sykmeldingDtoKonverterer.konverterBehandlingsutfall(sykmelding.validation)
            dto.merknader `should be equal to` sykmeldingDtoKonverterer.konverterMerknader(sykmelding.validation)

            dto.arbeidsgiver.`should be null`()
            dto.prognose.`should be null`()
            dto.behandler.`should be null`()
            dto.utenlandskSykmelding.`should be null`()
        }

        @Test
        fun `burde konvertere norsk sykmelding`() {
            val sykmelding =
                lagSykmelding(
                    sykmeldingGrunnlag =
                        lagSykmeldingGrunnlag(
                            medisinskVurdering = lagIkkeDigitalMedisinskVurdering(syketilfelleStartDato = LocalDate.parse("2025-01-01")),
                            pasient = lagPasient(navnFastlege = "Fastlege Navn"),
                            tilbakedatering = lagTilbakedatering(LocalDate.parse("2025-04-25")),
                        ),
                    validation = lagValidation(status = RuleType.PENDING),
                )

            val sykmeldingGrunnlag = sykmelding.sykmeldingGrunnlag.shouldBeInstanceOf<NorskSykmeldingGrunnlag>()

            val dto = sykmeldingDtoKonverterer.konverter(sykmelding)

            dto.pasient `should be equal to`
                sykmeldingDtoKonverterer.konverterPasient(
                    sykmeldingGrunnlag.pasient,
                    dto.sykmeldingsperioder.minBy { it.fom }.fom,
                )
            dto.medisinskVurdering `should be equal to`
                sykmeldingDtoKonverterer.konverterMedisinskVurdering(
                    sykmeldingGrunnlag.medisinskVurdering,
                )
            dto.arbeidsgiver `should be equal to` sykmeldingGrunnlag.arbeidsgiver.tilArbeidsgiverDTO()
            dto.prognose `should be equal to`
                sykmeldingGrunnlag.prognose?.let {
                    sykmeldingDtoKonverterer.konverterPrognose(it)
                }
            dto.utdypendeOpplysninger `should be equal to`
                sykmeldingDtoKonverterer.konverterUtdypendeOpplysninger(sykmeldingGrunnlag.utdypendeOpplysninger)
            dto.kontaktMedPasient `should be equal to`
                sykmeldingGrunnlag.tilbakedatering.`should not be null`().let {
                    sykmeldingDtoKonverterer.konverterKontaktMedPasient(it)
                }
            dto.behandler `should be equal to` sykmeldingDtoKonverterer.konverterBehandler(sykmeldingGrunnlag.behandler)

            dto.tiltakArbeidsplassen `should be equal to` sykmeldingGrunnlag.arbeidsgiver.getTiltakArbeidsplassen()
            dto.tiltakNAV `should be equal to` sykmeldingGrunnlag.tiltak?.tiltakNav
            dto.andreTiltak `should be equal to` sykmeldingGrunnlag.tiltak?.andreTiltak
            dto.meldingTilNAV `should be equal to`
                sykmeldingGrunnlag.bistandNav!!.let {
                    sykmeldingDtoKonverterer.konverterMeldingTilNAV(it)
                }
            dto.meldingTilArbeidsgiver `should be equal to` sykmeldingGrunnlag.arbeidsgiver.getMeldingTilArbeidsgiver()

            dto.utenlandskSykmelding.`should be null`()
        }

        @Test
        fun `burde konvertere utenlandsk sykmelding`() {
            val sykmelding = lagSykmelding(sykmeldingGrunnlag = lagUtenlandskSykmeldingGrunnlag())
            val sykmeldingGrunnlag = sykmelding.sykmeldingGrunnlag.shouldBeInstanceOf<UtenlandskSykmeldingGrunnlag>()

            val dto = sykmeldingDtoKonverterer.konverter(sykmelding)

            dto.pasient `should be equal to`
                sykmeldingDtoKonverterer.konverterPasient(
                    sykmelding.sykmeldingGrunnlag.pasient,
                    dto.sykmeldingsperioder.minBy { it.fom }.fom,
                )
            dto.medisinskVurdering `should be equal to`
                sykmeldingDtoKonverterer.konverterMedisinskVurdering(
                    sykmelding.sykmeldingGrunnlag.medisinskVurdering,
                )
            dto.utenlandskSykmelding!!.land `should be equal to` sykmeldingGrunnlag.utenlandskInfo.land

            dto.arbeidsgiver.`should be null`()
            dto.prognose.`should be null`()
            dto.behandler.`should be null`()
            dto.meldingTilNAV.`should be null`()
            dto.meldingTilArbeidsgiver.`should be null`()
            dto.kontaktMedPasient.`should be null`()
        }
    }

    @Nested
    inner class PasientTest {
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
                PasientDTO(fnr = "fnr", fornavn = "fornavn", mellomnavn = "mellomnavn", etternavn = "etternavn", overSyttiAar = false)
        }

        @Test
        fun `burde konvertere pasient som er over 70 år`() {
            pdlClient.setFoedselsdato(LocalDate.parse("1950-01-01"), "fnr")
            val pasient = Pasient(Navn("fornavn", "mellomnavn", "etternavn"), null, null, "fnr", emptyList())

            val pasientDto = sykmeldingDtoKonverterer.konverterPasient(pasient, LocalDate.parse("2025-01-01"))

            pasientDto.overSyttiAar `should be` true
        }
    }

    @Nested
    inner class ArbeidsgiverTest {
        @Test fun `burde konvertere arbeidsgiver, en arbeidsgiver`() {
            EnArbeidsgiver("Navn", "_", 50, "_", "_").tilArbeidsgiverDTO() shouldBeEqualTo
                ArbeidsgiverDTO("Navn", 50)
        }

        @Test fun `burde konvertere arbeidsgiver, flere arbeidsgiver`() {
            FlereArbeidsgivere("Navn", "_", 50, "_", "_").tilArbeidsgiverDTO() shouldBeEqualTo
                ArbeidsgiverDTO("Navn", 50)
        }

        @Test fun `burde konvertere arbeidsgiver, ingen arbeidsgiver`() {
            IngenArbeidsgiver().tilArbeidsgiverDTO().`should be null`()
        }
    }

    @Test
    fun `burde bruke genDato dersom behandletTidspunkt ikke er satt`() {
        val sykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        metadata =
                            lagSykmeldingMetadata(
                                genDato = OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                                behandletTidspunkt = null,
                            ),
                    ),
            )

        val dto = sykmeldingDtoKonverterer.konverter(sykmelding)

        dto.behandletTidspunkt `should be equal to` OffsetDateTime.parse("2025-01-01T00:00:00Z")
    }

    @Test
    fun `burde konvertere aktivitet til en periode`() {
        val aktivitet =
            AktivitetIkkeMulig(
                medisinskArsak =
                    MedisinskArsak(
                        beskrivelse = "",
                        arsak = listOf(MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING),
                    ),
                arbeidsrelatertArsak =
                    ArbeidsrelatertArsak(
                        beskrivelse = "",
                        arsak = listOf(ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING),
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
    fun `burde konvertere ikke-digital medisinsk vurdering`() {
        val medisinskVurdering =
            IkkeDigitalMedisinskVurdering(
                hovedDiagnose =
                    DiagnoseInfo(
                        system = DiagnoseSystem.ICD10,
                        kode = "kode",
                        tekst = "tekst",
                    ),
                biDiagnoser =
                    listOf(
                        DiagnoseInfo(
                            system = DiagnoseSystem.ICPC2,
                            kode = "bi diagnose",
                            tekst = null,
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

        sykmeldingDtoKonverterer.konverterMedisinskVurdering(medisinskVurdering) `should be equal to`
            MedisinskVurderingDTO(
                hovedDiagnose =
                    DiagnoseDTO(
                        kode = "kode",
                        system = "ICD10",
                        tekst = "tekst",
                    ),
                biDiagnoser =
                    listOf(
                        DiagnoseDTO(
                            kode = "bi diagnose",
                            system = "ICPC2",
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
    }

    @Test
    fun `burde konvertere digital medisinsk vurdering`() {
        val medisinskVurdering =
            DigitalMedisinskVurdering(
                hovedDiagnose =
                    DiagnoseInfo(
                        system = DiagnoseSystem.ICD10,
                        kode = "kode",
                        tekst = "tekst",
                    ),
                biDiagnoser =
                    listOf(
                        DiagnoseInfo(
                            system = DiagnoseSystem.ICPC2,
                            kode = "bi diagnose",
                            tekst = null,
                        ),
                    ),
                svangerskap = true,
                yrkesskade =
                    Yrkesskade(
                        yrkesskadeDato = LocalDate.parse("2021-01-01"),
                    ),
                skjermetForPasient = false,
                syketilfelletStartDato = LocalDate.parse("2021-01-01"),
                annenFravarsgrunn = AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON,
            )

        sykmeldingDtoKonverterer.konverterMedisinskVurdering(medisinskVurdering) `should be equal to`
            MedisinskVurderingDTO(
                hovedDiagnose =
                    DiagnoseDTO(
                        kode = "kode",
                        system = "ICD10",
                        tekst = "tekst",
                    ),
                biDiagnoser =
                    listOf(
                        DiagnoseDTO(
                            kode = "bi diagnose",
                            system = "ICPC2",
                            tekst = null,
                        ),
                    ),
                annenFraversArsak =
                    AnnenFraversArsakDTO(
                        beskrivelse = null,
                        grunn = listOf(AnnenFraverGrunnDTO.GODKJENT_HELSEINSTITUSJON),
                    ),
                svangerskap = true,
                yrkesskade = true,
                yrkesskadeDato = LocalDate.parse("2021-01-01"),
            )
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
        EnArbeidsgiver(
            navn = "_",
            yrkesbetegnelse = "_",
            stillingsprosent = 0,
            meldingTilArbeidsgiver = "_",
            tiltakArbeidsplassen = "tiltak",
        ).getTiltakArbeidsplassen() `should be equal to` "tiltak"

        FlereArbeidsgivere(
            meldingTilArbeidsgiver = "_",
            tiltakArbeidsplassen = "tiltak",
            navn = "_",
            yrkesbetegnelse = "_",
            stillingsprosent = 0,
        ).getTiltakArbeidsplassen() `should be equal to` "tiltak"

        IngenArbeidsgiver().getTiltakArbeidsplassen().`should be null`()
    }

    @Test
    fun `burde konvertere melding til arbeidsgiver`() {
        EnArbeidsgiver(
            meldingTilArbeidsgiver = "Melding",
            tiltakArbeidsplassen = "_",
            navn = "_",
            yrkesbetegnelse = "_",
            stillingsprosent = 0,
        ).getMeldingTilArbeidsgiver() `should be equal to` "Melding"

        FlereArbeidsgivere(
            meldingTilArbeidsgiver = "Melding",
            tiltakArbeidsplassen = "_",
            navn = "_",
            yrkesbetegnelse = "_",
            stillingsprosent = 0,
        ).getMeldingTilArbeidsgiver() `should be equal to` "Melding"

        IngenArbeidsgiver().getMeldingTilArbeidsgiver().`should be null`()
    }

    @Test
    fun `burde konvertere kontakt med pasient`() {
        sykmeldingDtoKonverterer.konverterKontaktMedPasient(lagSykmeldingGrunnlag().tilbakedatering!!).`should not be null`()
    }

    @Test
    fun `burde konvertere behandlingsutfall type INVALID`() {
        val validationResult =
            ValidationResult(
                status = RuleType.INVALID,
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                rules =
                    listOf(
                        InvalidRule(
                            name = "BEHANDLER_MANGLER_AUTORISASJON_I_HPR",
                            timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                            validationType = ValidationType.MANUAL,
                            reason =
                                Reason(
                                    sykmeldt =
                                        "Den som har skrevet sykmeldingen, har ikke autorisasjon til å gjøre det." +
                                            " Du må derfor få en annen til å skrive sykmeldingen",
                                    sykmelder = "Behandleren har ikke autorisasjon i HPR",
                                ),
                        ),
                    ),
            )

        val konvertertBehandlingsutfall =
            sykmeldingDtoKonverterer.konverterBehandlingsutfall(validationResult)
        konvertertBehandlingsutfall `should be equal to`
            BehandlingsutfallDTO(
                status = RegelStatusDTO.INVALID,
                ruleHits =
                    listOf(
                        RegelinfoDTO(
                            ruleStatus = RegelStatusDTO.INVALID,
                            messageForSender = "Behandleren har ikke autorisasjon i HPR",
                            messageForUser =
                                "Den som har skrevet sykmeldingen, har ikke autorisasjon til å gjøre det." +
                                    " Du må derfor få en annen til å skrive sykmeldingen",
                            ruleName = "BEHANDLER_MANGLER_AUTORISASJON_I_HPR",
                        ),
                    ),
                erUnderBehandling = false,
            )
    }

    @Test
    fun `burde konvertere behandlingsutfall type PENDING`() {
        val validationResult =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                rules = emptyList(),
            )

        val konvertertBehandlingsutfall =
            sykmeldingDtoKonverterer.konverterBehandlingsutfall(validationResult)
        konvertertBehandlingsutfall `should be equal to`
            BehandlingsutfallDTO(
                status = RegelStatusDTO.OK,
                ruleHits = emptyList(),
                erUnderBehandling = true,
            )
    }

    @Test
    fun `konverterMerknader burde konvertere én fra validationResult`() {
        val validationResult =
            ValidationResult(
                status = RuleType.PENDING,
                timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                rules =
                    listOf(
                        PendingRule(
                            name = "TILBAKEDATERING_DELVIS_GODKJENT",
                            timestamp = OffsetDateTime.parse("2021-01-01T00:00:00Z"),
                            reason =
                                Reason(
                                    sykmeldt = "Sykmeldingen er delvis godkjent",
                                    sykmelder = "",
                                ),
                        ),
                    ),
            )
        val merknader = sykmeldingDtoKonverterer.konverterMerknader(validationResult)
        merknader
            .shouldHaveSize(1)
            .first()
            .run {
                this.type `should be equal to` MerknadtypeDTO.DELVIS_GODKJENT
                this.beskrivelse `should be equal to` "Sykmeldingen er delvis godkjent"
            }
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
