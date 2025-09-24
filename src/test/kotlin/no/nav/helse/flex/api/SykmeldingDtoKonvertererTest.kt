package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.domain.tsm.*
import no.nav.helse.flex.sykmelding.domain.tsm.values.*
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

private class AssertionCollection<A> {
    private val tests = mutableListOf<A.() -> Unit>()

    fun add(test: A.() -> Unit) {
        tests.add(test)
    }

    fun runAllOn(actual: A) {
        tests.forEach { it(actual) }
    }
}

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
    fun `burde konvertere sykmelding`() {
        val outputAssertions = AssertionCollection<SykmeldingDTO>()

        val sykmelding =
            lagSykmelding(
                sykmeldingGrunnlag =
                    lagSykmeldingGrunnlag(
                        id =
                            "1".also {
                                outputAssertions.add { id shouldBeEqualTo it }
                            },
                        metadata =
                            lagSykmeldingMetadata(
                                mottattDato =
                                    OffsetDateTime
                                        .parse("2021-07-07T12:30:00.000Z")
                                        .also {
                                            outputAssertions.add { mottattTidspunkt shouldBeEqualTo it }
                                        },
                                behandletTidspunkt =
                                    OffsetDateTime
                                        .parse("2021-07-07T12:30:00.000Z")
                                        .also {
                                            outputAssertions.add { behandletTidspunkt shouldBeEqualTo it }
                                        },
                                regelsettVersjon =
                                    "1.2".also {
                                        outputAssertions.add { rulesetVersion shouldBeEqualTo it }
                                    },
                            ),
                        pasient =
                            lagPasient(
                                fnr = "fnr",
                                navn =
                                    Navn(
                                        fornavn = "Ola",
                                        mellomnavn = "Mellomnavn",
                                        etternavn = "Nordmann",
                                    ),
                            ).also {
                                outputAssertions.add {
                                    pasient shouldBeEqualTo
                                        PasientDTO(
                                            fnr = "fnr",
                                            fornavn = "Ola",
                                            mellomnavn = "Mellomnavn",
                                            etternavn = "Nordmann",
                                            overSyttiAar = false,
                                        )
                                }
                            },
                        medisinskVurdering =
                            lagMedisinskVurdering(
                                syketilfelleStartDato = LocalDate.parse("2025-01-01"),
                            ),
                        tilbakedatering =
                            lagTilbakedatering(
                                LocalDate.parse("2025-04-25"),
                            ),
                    ),
            )

        val sykmeldingDTO = sykmeldingDtoKonverterer.konverter(sykmelding)

        outputAssertions.runAllOn(sykmeldingDTO)

//        val sykmelding =
//            lagSykmelding(
//                sykmeldingGrunnlag =
//                    lagSykmeldingGrunnlag(
//                        medisinskVurdering =
//                            lagMedisinskVurdering(
//                                syketilfelleStartDato = LocalDate.parse("2025-01-01"),
//                            ),
//                        pasient = lagPasient(navnFastlege = "Fastlege Navn"),
//                        tilbakedatering =
//                            lagTilbakedatering(
//                                LocalDate.parse("2025-04-25"),
//                            ),
//                    ),
//                validation = lagValidation(status = RuleType.PENDING),
//            )
//
//        val dto = sykmeldingDtoKonverterer.konverter(sykmelding)
//
//        dto.id `should be equal to` sykmelding.sykmeldingId
//
//        dto.pasient `should be equal to`
//            PasientDTO(
//                fnr = sykmelding.sykmeldingGrunnlag.pasient.fnr,
//                fornavn =
//                    sykmelding.sykmeldingGrunnlag.pasient.navn
//                        ?.fornavn,
//                mellomnavn =
//                    sykmelding.sykmeldingGrunnlag.pasient.navn
//                        ?.mellomnavn,
//                etternavn =
//                    sykmelding.sykmeldingGrunnlag.pasient.navn
//                        ?.etternavn,
//                overSyttiAar = false,
//            )
//
//        dto.mottattTidspunkt `should be equal to` sykmelding.sykmeldingGrunnlag.metadata.mottattDato
//
//        dto.behandlingsutfall `should be equal to`
//            BehandlingsutfallDTO(
//                status = RegelStatusDTO.OK,
//                ruleHits = emptyList(),
//            )
//
//        dto.legekontorOrgnummer.`should be null`()
//
//        dto.arbeidsgiver `should be equal to`
//            ArbeidsgiverDTO(
//                navn = "Arbeidsgivernavn",
//                stillingsprosent = 99,
//            )
//
//        dto.sykmeldingsperioder.shouldHaveSize(1)
//        dto.sykmeldingsperioder.first().run {
//            fom `should be equal to`
//                sykmelding.sykmeldingGrunnlag.aktivitet
//                    .first()
//                    .fom
//            tom `should be equal to`
//                sykmelding.sykmeldingGrunnlag.aktivitet
//                    .first()
//                    .tom
//            type `should be equal to` PeriodetypeDTO.AKTIVITET_IKKE_MULIG
//            aktivitetIkkeMulig!!.run {
//                medisinskArsak `should be equal to`
//                    MedisinskArsakDTO(
//                        beskrivelse = "Pasient er syk",
//                        arsak = listOf(MedisinskArsakTypeDTO.TILSTAND_HINDRER_AKTIVITET),
//                    )
//                arbeidsrelatertArsak.`should be null`()
//            }
//            gradert.`should be null`()
//            behandlingsdager.`should be null`()
//            reisetilskudd `should be equal to` false
//            innspillTilArbeidsgiver.`should be null`()
//        }
//
//        dto.sykmeldingStatus.run {
//            statusEvent `should be equal to` "APEN"
//            timestamp `should be equal to` OffsetDateTime.ofInstant(sykmelding.opprettet, ZoneOffset.UTC)
//            brukerSvar.`should be null`()
//            arbeidsgiver.`should be null`()
//        }
//
//        dto.medisinskVurdering!!.run {
//            hovedDiagnose `should be equal to`
//                DiagnoseDTO(
//                    kode = "R51",
//                    system = "ICPC2",
//                    tekst = "tekst",
//                )
//            biDiagnoser.shouldHaveSize(1)
//            biDiagnoser.first() `should be equal to`
//                DiagnoseDTO(
//                    kode = "J06.9",
//                    system = "ICD10",
//                    tekst = "tekst",
//                )
//            annenFraversArsak.`should be null`()
//            svangerskap `should be equal to` false
//            yrkesskade `should be equal to` false
//            yrkesskadeDato.`should be null`()
//        }
//
//        dto.skjermesForPasient `should be equal to` false
//
//        dto.prognose!!.run {
//            arbeidsforEtterPeriode `should be equal to` true
//            hensynArbeidsplassen `should be equal to` "Tilrettelegging på arbeidsplassen anbefales"
//            erIArbeid.`should be null`()
//            erIkkeIArbeid.`should be null`()
//        }
//
//        dto.utdypendeOpplysninger.shouldHaveSize(1)
//        dto.utdypendeOpplysninger["arbeidsforhold"]!!.run {
//            shouldHaveSize(1)
//            get("tilrettelegging")!! `should be equal to`
//                SporsmalSvarDTO(
//                    sporsmal = "Har du behov for tilrettelegging?",
//                    svar = "Ja",
//                    restriksjoner = listOf(SvarRestriksjonDTO.SKJERMET_FOR_ARBEIDSGIVER),
//                )
//        }
//
//        dto.tiltakArbeidsplassen `should be equal to` "Dette er et tiltak"
//        dto.tiltakNAV `should be equal to` "Behov for tilrettelegging"
//        dto.andreTiltak `should be equal to` "Redusert arbeidstid"
//
//        dto.meldingTilNAV!!.run {
//            bistandUmiddelbart `should be equal to` false
//            beskrivBistand `should be equal to` "Ingen behov for bistand per nå"
//        }
//
//        dto.meldingTilArbeidsgiver `should be equal to` "Melding til arbeidsgiver"
//
//        dto.kontaktMedPasient `should be equal to`
//            KontaktMedPasientDTO(
//                kontaktDato = LocalDate.parse("2025-04-25"),
//                begrunnelseIkkeKontakt = "Pasienten kunne ikke oppsøke lege tidligere",
//            )
//
//        dto.behandletTidspunkt `should be equal to` sykmelding.sykmeldingGrunnlag.metadata.behandletTidspunkt
//
//        dto.behandler!!.run {
//            fornavn `should be equal to` "Kari"
//            mellomnavn.`should be null`()
//            etternavn `should be equal to` "Hansen"
//            adresse `should be equal to`
//                AdresseDTO(
//                    gate = "Hovedgaten 1",
//                    postnummer = 101,
//                    kommune = "Oslo",
//                    postboks = null,
//                    land = "Norge",
//                )
//            tlf `should be equal to` "11111111"
//        }
//
//        dto.syketilfelleStartDato `should be equal to` LocalDate.parse("2025-01-01")
//        dto.navnFastlege `should be equal to` "Fastlege Navn"
//        dto.egenmeldt `should be equal to` false
//        dto.papirsykmelding `should be equal to` false
//        dto.harRedusertArbeidsgiverperiode `should be equal to` false
//
//        dto.merknader!!.shouldHaveSize(1)
//        dto.merknader!!.first() `should be equal to`
//            MerknadDTO(
//                type = MerknadtypeDTO.UNDER_BEHANDLING,
//                beskrivelse = "Sykmeldingen blir manuelt behandlet fordi den er tilbakedatert",
//            )
//
//        dto.rulesetVersion `should be equal to` sykmelding.sykmeldingGrunnlag.metadata.regelsettVersjon
//        dto.utenlandskSykmelding.`should be null`()
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
    fun `burde konvertere arbeidsgiver, en arbeidsgiver`() {
        val enArbeidsgiver =
            EnArbeidsgiver(
                navn = "Navn",
                yrkesbetegnelse = "_",
                stillingsprosent = 50,
                meldingTilArbeidsgiver = "_",
                tiltakArbeidsplassen = "_",
            )

        enArbeidsgiver.tilArbeidsgiverDTO() shouldBeEqualTo
            ArbeidsgiverDTO(
                navn = "Navn",
                stillingsprosent = 50,
            )
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

        arbeidsgiver.tilArbeidsgiverDTO() `should be equal to` forventetArbeidsgiver
    }

    @Test
    fun `burde konvertere arbeidsgiver, ingen arbeidsgiver`() {
        IngenArbeidsgiver().tilArbeidsgiverDTO().`should be null`()
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
    fun `burde konvertere medisinsk vurdering`() {
        val medisinskVurdering =
            MedisinskVurdering(
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

        IngenArbeidsgiver().getTiltakArbeidsplassen() `should be equal to` null
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

        IngenArbeidsgiver().getMeldingTilArbeidsgiver() `should be equal to` null
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
                            name = RuleNameDTO.BEHANDLER_MANGLER_AUTORISASJON_I_HPR.name,
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
                            ruleName = RuleNameDTO.BEHANDLER_MANGLER_AUTORISASJON_I_HPR.name,
                        ),
                    ),
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
