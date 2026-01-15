package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.tsm.*
import no.nav.helse.flex.sykmelding.tsm.values.*
import java.time.LocalDate
import java.time.OffsetDateTime

fun lagSykmeldingGrunnlag(
    id: String = "1",
    pasient: Pasient = lagPasient(),
    aktiviteter: List<Aktivitet> =
        listOf(
            lagAktivitetIkkeMulig(
                LocalDate.parse("2021-01-01"),
                LocalDate.parse("2021-01-10"),
            ),
        ),
    metadata: SykmeldingMetadata = lagSykmeldingMetadata(),
    medisinskVurdering: MedisinskVurdering = lagMedisinskVurdering(),
    tilbakedatering: Tilbakedatering? = lagTilbakedatering(),
): XMLSykmeldingGrunnlag =
    XMLSykmeldingGrunnlag(
        id = id,
        metadata = metadata,
        pasient = pasient,
        medisinskVurdering = medisinskVurdering,
        aktivitet = aktiviteter,
        behandler = lagBehandler(),
        arbeidsgiver =
            FlereArbeidsgivere(
                meldingTilArbeidsgiver = "Melding til arbeidsgiver",
                tiltakArbeidsplassen = "Dette er et tiltak",
                navn = "Arbeidsgivernavn",
                yrkesbetegnelse = "Arbeider",
                stillingsprosent = 99,
            ),
        sykmelder =
            Sykmelder(
                ids =
                    listOf(
                        PersonId(id = "00000000000", type = PersonIdType.DKF),
                    ),
                helsepersonellKategori = HelsepersonellKategori.LEGE,
            ),
        prognose =
            Prognose(
                arbeidsforEtterPeriode = true,
                hensynArbeidsplassen = "Tilrettelegging på arbeidsplassen anbefales",
                arbeid = null,
            ),
        tiltak =
            Tiltak(
                tiltakNav = "Behov for tilrettelegging",
                andreTiltak = "Redusert arbeidstid",
            ),
        bistandNav =
            BistandNav(
                bistandUmiddelbart = false,
                beskrivBistand = "Ingen behov for bistand per nå",
            ),
        tilbakedatering = tilbakedatering,
        utdypendeOpplysninger =
            mapOf(
                "arbeidsforhold" to
                    mapOf(
                        "tilrettelegging" to
                            SporsmalSvar(
                                sporsmal = "Har du behov for tilrettelegging?",
                                svar = "Ja",
                                restriksjoner = listOf(SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER),
                            ),
                    ),
            ),
    )

fun lagTilbakedatering(kontaktDato: LocalDate? = LocalDate.parse("2025-01-01")): Tilbakedatering =
    Tilbakedatering(
        kontaktDato = kontaktDato,
        begrunnelse = "Pasienten kunne ikke oppsøke lege tidligere",
    )

fun lagUtenlandskSykmeldingGrunnlag(id: String = "1"): UtenlandskSykmeldingGrunnlag =
    UtenlandskSykmeldingGrunnlag(
        id = id,
        metadata = lagSykmeldingMetadata(),
        pasient = lagPasient(),
        medisinskVurdering = lagMedisinskVurdering(),
        aktivitet = listOf(lagAktivitetIkkeMulig(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-01-10"))),
        utenlandskInfo =
            UtenlandskInfo(
                land = "Sverige",
                folkeRegistertAdresseErBrakkeEllerTilsvarende = false,
                erAdresseUtland = false,
            ),
    )

fun lagDigitalSykmeldingGrunnlag(id: String = "1"): DigitalSykmeldingGrunnlag =
    DigitalSykmeldingGrunnlag(
        id = id,
        metadata = lagSykmeldingMetadata(),
        pasient = lagPasient(),
        medisinskVurdering = lagMedisinskVurdering(),
        aktivitet = listOf(lagAktivitetIkkeMulig(LocalDate.parse("2021-01-01"), LocalDate.parse("2021-01-10"))),
        behandler = lagBehandler(),
        arbeidsgiver = IngenArbeidsgiver(),
        sykmelder =
            Sykmelder(
                ids =
                    listOf(
                        PersonId(id = "00000000000", type = PersonIdType.FNR),
                    ),
                helsepersonellKategori = HelsepersonellKategori.LEGE,
            ),
        bistandNav = null,
        tilbakedatering = null,
        utdypendeSporsmal =
            listOf(
                UtdypendeSporsmal(
                    type = Sporsmalstype.MEDISINSK_OPPSUMMERING,
                    svar = "svar",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                    svar = "svar",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                    svar = "svar",
                ),
            ),
    )

fun lagSykmeldingMetadata(
    avsenderSystem: AvsenderSystem =
        AvsenderSystem(
            navn = AvsenderSystemNavn.PRIDOK_EPJ,
            versjon = "2.1.0",
        ),
    mottattDato: OffsetDateTime = OffsetDateTime.now(),
    genDato: OffsetDateTime = OffsetDateTime.now().minusDays(1),
    behandletTidspunkt: OffsetDateTime? = OffsetDateTime.now().minusHours(2),
): SykmeldingMetadata =
    SykmeldingMetadata(
        mottattDato = mottattDato,
        genDate = genDato,
        behandletTidspunkt = behandletTidspunkt,
        regelsettVersjon = "1.0",
        avsenderSystem = avsenderSystem,
        strekkode = "ABC12345",
    )

fun lagPasient(
    fnr: String = "01010112345",
    navnFastlege: String? = null,
): Pasient =
    Pasient(
        fnr = fnr,
        navn =
            Navn(
                fornavn = "Ola",
                mellomnavn = null,
                etternavn = "Nordmann",
            ),
        kontaktinfo =
            listOf(
                Kontaktinfo(type = KontaktinfoType.TLF, value = "11111111"),
            ),
        navKontor = null,
        navnFastlege = navnFastlege,
    )

fun lagMedisinskVurdering(
    hovedDiagnoseKode: String = "R51",
    annenFraverArsak: AnnenFraverArsak? = null,
    syketilfelleStartDato: LocalDate? = null,
): MedisinskVurdering =
    MedisinskVurdering(
        hovedDiagnose =
            DiagnoseInfo(
                system = DiagnoseSystem.ICPC2,
                kode = hovedDiagnoseKode,
                tekst = "tekst",
            ),
        biDiagnoser =
            listOf(
                DiagnoseInfo(
                    system = DiagnoseSystem.ICD10,
                    kode = "J06.9",
                    tekst = "tekst",
                ),
            ),
        svangerskap = false,
        annenFraversArsak = annenFraverArsak,
        yrkesskade = null,
        skjermetForPasient = false,
        annenFravarsgrunn = null,
        syketilfelletStartDato = syketilfelleStartDato,
    )

fun lagAktivitetBehandlingsdager(): Behandlingsdager =
    Behandlingsdager(
        antallBehandlingsdager = 1,
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
    )

fun lagAktivitetGradert(): Gradert =
    Gradert(
        grad = 1,
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
        reisetilskudd = false,
    )

fun lagAktivitetReisetilskudd(): Reisetilskudd =
    Reisetilskudd(
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
    )

fun lagAktivitetAvventende(): Avventende =
    Avventende(
        innspillTilArbeidsgiver = "Ingen",
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
    )

fun lagAktivitetIkkeMulig(
    fom: LocalDate = LocalDate.now().minusDays(1),
    tom: LocalDate = LocalDate.now().plusDays(1),
) = AktivitetIkkeMulig(
    medisinskArsak =
        MedisinskArsak(
            arsak = listOf(MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET),
            beskrivelse = "Pasient er syk",
        ),
    arbeidsrelatertArsak = null,
    fom = fom,
    tom = tom,
)

fun lagBehandler() =
    Behandler(
        ids =
            listOf(
                PersonId(id = "00000000000", type = PersonIdType.DKF),
            ),
        navn =
            Navn(
                fornavn = "Kari",
                mellomnavn = null,
                etternavn = "Hansen",
            ),
        kontaktinfo =
            listOf(
                Kontaktinfo(
                    type = KontaktinfoType.TLF,
                    value = "11111111",
                ),
            ),
        adresse =
            Adresse(
                type = AdresseType.BOSTEDSADRESSE,
                gateadresse = "Hovedgaten 1",
                postnummer = "0101",
                poststed = "Oslo",
                postboks = null,
                kommune = "Oslo",
                land = "Norge",
            ),
    )
