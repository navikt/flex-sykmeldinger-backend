package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.tsm.*
import no.nav.helse.flex.sykmelding.tsm.values.*
import java.time.LocalDate
import java.time.OffsetDateTime

fun lagSykmeldingGrunnlag(
    id: String = "1",
    pasient: Pasient = lagPasient(),
    aktiviteter: List<Aktivitet> = listOf(lagAktivitetIkkeMulig()),
    metadata: SykmeldingMetadata = lagSykmeldingMetadata(),
    medisinskVurdering: IkkeDigitalMedisinskVurdering = lagIkkeDigitalMedisinskVurdering(),
): SykmeldingGrunnlag =
    lagNorskSykmeldingGrunnlag(
        id = id,
        metadata = metadata,
        pasient = pasient,
        medisinskVurdering = medisinskVurdering,
        aktiviteter = aktiviteter,
        tilbakedatering = null,
    )

fun lagNorskSykmeldingGrunnlag(
    id: String = "1",
    pasient: Pasient = lagPasient(),
    aktiviteter: List<Aktivitet> = listOf(lagAktivitetIkkeMulig()),
    metadata: SykmeldingMetadata = lagSykmeldingMetadata(),
    medisinskVurdering: IkkeDigitalMedisinskVurdering = lagIkkeDigitalMedisinskVurdering(),
    tilbakedatering: Tilbakedatering? = lagTilbakedatering(),
    behandler: Behandler = lagBehandler(),
    arbeidsgiver: ArbeidsgiverInfo = lagArbeidsgiverInfoFlereArbeidsgivere(),
    prognose: Prognose? = lagPrognose(),
    tiltak: Tiltak = lagTiltak(),
    bistandNav: BistandNav = lagBistandNav(),
): NorskSykmeldingGrunnlag =
    lagXMLSykmeldingGrunnlag(
        id = id,
        metadata = metadata,
        pasient = pasient,
        medisinskVurdering = medisinskVurdering,
        aktiviteter = aktiviteter,
        tilbakedatering = tilbakedatering,
        behandler = behandler,
        arbeidsgiver = arbeidsgiver,
        prognose = prognose,
        tiltak = tiltak,
        bistandNav = bistandNav,
    )

fun lagXMLSykmeldingGrunnlag(
    id: String = "1",
    pasient: Pasient = lagPasient(),
    aktiviteter: List<Aktivitet> = listOf(lagAktivitetIkkeMulig()),
    metadata: SykmeldingMetadata = lagSykmeldingMetadata(),
    medisinskVurdering: IkkeDigitalMedisinskVurdering = lagIkkeDigitalMedisinskVurdering(),
    tilbakedatering: Tilbakedatering? = lagTilbakedatering(),
    behandler: Behandler = lagBehandler(),
    arbeidsgiver: ArbeidsgiverInfo = lagArbeidsgiverInfoFlereArbeidsgivere(),
    prognose: Prognose? = lagPrognose(),
    tiltak: Tiltak = lagTiltak(),
    bistandNav: BistandNav = lagBistandNav(),
): XMLSykmeldingGrunnlag =
    XMLSykmeldingGrunnlag(
        id = id,
        metadata = metadata,
        pasient = pasient,
        medisinskVurdering = medisinskVurdering,
        aktivitet = aktiviteter,
        behandler = behandler,
        arbeidsgiver = arbeidsgiver,
        sykmelder =
            Sykmelder(
                ids =
                    listOf(
                        PersonId(id = "00000000000", type = PersonIdType.DKF),
                    ),
                helsepersonellKategori = HelsepersonellKategori.LEGE,
            ),
        prognose = prognose,
        tiltak = tiltak,
        bistandNav = bistandNav,
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

fun lagUtenlandskInfo(
    land: String = "Sverige",
    folkeRegistertAdresseErBrakkeEllerTilsvarende: Boolean = false,
    erAdresseUtland: Boolean = false,
): UtenlandskInfo =
    UtenlandskInfo(
        land = land,
        folkeRegistertAdresseErBrakkeEllerTilsvarende = folkeRegistertAdresseErBrakkeEllerTilsvarende,
        erAdresseUtland = erAdresseUtland,
    )

fun lagUtenlandskSykmeldingGrunnlag(
    id: String = "1",
    metadata: SykmeldingMetadata = lagSykmeldingMetadata(),
    pasient: Pasient = lagPasient(),
    medisinskVurdering: IkkeDigitalMedisinskVurdering = lagIkkeDigitalMedisinskVurdering(),
    aktivitet: List<Aktivitet> = listOf(lagAktivitetIkkeMulig()),
    utenlandskInfo: UtenlandskInfo = lagUtenlandskInfo(),
): UtenlandskSykmeldingGrunnlag =
    UtenlandskSykmeldingGrunnlag(
        id = id,
        metadata = metadata,
        pasient = pasient,
        medisinskVurdering = medisinskVurdering,
        aktivitet = aktivitet,
        utenlandskInfo = utenlandskInfo,
    )

fun lagDigitalSykmeldingGrunnlag(id: String = "1"): DigitalSykmeldingGrunnlag =
    DigitalSykmeldingGrunnlag(
        id = id,
        metadata = lagSykmeldingMetadata(),
        pasient = lagPasient(),
        medisinskVurdering = lagDigitalMedisinskVurdering(),
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
                    sporsmal = "sporsmal",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.UTFORDRINGER_MED_GRADERT_ARBEID,
                    svar = "svar",
                    sporsmal = "sporsmal",
                ),
                UtdypendeSporsmal(
                    type = Sporsmalstype.HENSYN_PA_ARBEIDSPLASSEN,
                    svar = "svar",
                    sporsmal = "sporsmal",
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

fun lagIkkeDigitalMedisinskVurdering(
    hovedDiagnoseKode: String = "R51",
    annenFraverArsak: AnnenFraverArsak? = null,
    syketilfelleStartDato: LocalDate? = null,
): IkkeDigitalMedisinskVurdering =
    IkkeDigitalMedisinskVurdering(
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
        syketilfelletStartDato = syketilfelleStartDato,
    )

fun lagDigitalMedisinskVurdering(
    hovedDiagnoseKode: String = "R51",
    annenfravarsgrunn: AnnenFravarArsakType? = null,
    syketilfelleStartDato: LocalDate? = null,
): DigitalMedisinskVurdering =
    DigitalMedisinskVurdering(
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
        yrkesskade = null,
        skjermetForPasient = false,
        annenFravarsgrunn = annenfravarsgrunn,
        syketilfelletStartDato = syketilfelleStartDato,
    )

fun lagAktivitetBehandlingsdager(
    fom: LocalDate = LocalDate.parse("2021-01-01"),
    tom: LocalDate = LocalDate.parse("2021-01-10"),
    antallBehandlingsdager: Int = 1,
): Behandlingsdager =
    Behandlingsdager(
        antallBehandlingsdager = antallBehandlingsdager,
        fom = fom,
        tom = tom,
    )

fun lagAktivitetGradert(
    fom: LocalDate = LocalDate.parse("2021-01-01"),
    tom: LocalDate = LocalDate.parse("2021-01-10"),
    grad: Int = 1,
    reisetilskudd: Boolean = false,
): Gradert =
    Gradert(
        grad = grad,
        fom = fom,
        tom = tom,
        reisetilskudd = reisetilskudd,
    )

fun lagAktivitetReisetilskudd(
    fom: LocalDate = LocalDate.parse("2021-01-01"),
    tom: LocalDate = LocalDate.parse("2021-01-10"),
): Reisetilskudd =
    Reisetilskudd(
        fom = fom,
        tom = tom,
    )

fun lagAktivitetAvventende(
    fom: LocalDate = LocalDate.parse("2021-01-01"),
    tom: LocalDate = LocalDate.parse("2021-01-10"),
    innspillTilArbeidsgiver: String = "Ingen",
): Avventende =
    Avventende(
        innspillTilArbeidsgiver = innspillTilArbeidsgiver,
        fom = fom,
        tom = tom,
    )

fun lagAktivitetIkkeMulig(
    fom: LocalDate = LocalDate.parse("2021-01-01"),
    tom: LocalDate = LocalDate.parse("2021-01-10"),
    medisinskArsak: MedisinskArsak? =
        MedisinskArsak(
            arsak = listOf(MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET),
            beskrivelse = "Pasient er syk",
        ),
    arbeidsrelatertArsak: ArbeidsrelatertArsak? = null,
) = AktivitetIkkeMulig(
    medisinskArsak = medisinskArsak,
    arbeidsrelatertArsak = arbeidsrelatertArsak,
    fom = fom,
    tom = tom,
)

fun lagAddresse(
    type: AdresseType = AdresseType.BOSTEDSADRESSE,
    gateadresse: String? = "Hovedgaten 1",
    postnummer: String? = "0101",
    poststed: String? = "Oslo",
    postboks: String? = null,
    kommune: String? = "Oslo",
    land: String? = "Norge",
): Adresse =
    Adresse(
        type = type,
        gateadresse = gateadresse,
        postnummer = postnummer,
        poststed = poststed,
        postboks = postboks,
        kommune = kommune,
        land = land,
    )

fun lagBehandler(
    navn: Navn =
        Navn(
            fornavn = "Kari",
            mellomnavn = null,
            etternavn = "Hansen",
        ),
    adresse: Adresse? = lagAddresse(),
    ids: List<PersonId> = listOf(PersonId(id = "00000000000", type = PersonIdType.DKF)),
    kontaktinfo: List<Kontaktinfo> =
        listOf(
            Kontaktinfo(
                type = KontaktinfoType.TLF,
                value = "11111111",
            ),
        ),
): Behandler =
    Behandler(
        navn = navn,
        adresse = adresse,
        ids = ids,
        kontaktinfo = kontaktinfo,
    )

fun lagArbeidsgiverInfoEnArbeidsgiver(
    meldingTilArbeidsgiver: String? = "Melding til arbeidsgiver",
    tiltakArbeidsplassen: String? = "Dette er et tiltak",
    navn: String? = "Arbeidsgivernavn",
    yrkesbetegnelse: String? = "Arbeider",
    stillingsprosent: Int? = 99,
): EnArbeidsgiver =
    EnArbeidsgiver(
        meldingTilArbeidsgiver = meldingTilArbeidsgiver,
        tiltakArbeidsplassen = tiltakArbeidsplassen,
        navn = navn,
        yrkesbetegnelse = yrkesbetegnelse,
        stillingsprosent = stillingsprosent,
    )

fun lagArbeidsgiverInfoFlereArbeidsgivere(
    meldingTilArbeidsgiver: String? = "Melding til arbeidsgiver",
    tiltakArbeidsplassen: String? = "Dette er et tiltak",
    navn: String? = "Arbeidsgivernavn",
    yrkesbetegnelse: String? = "Arbeider",
    stillingsprosent: Int? = 99,
): FlereArbeidsgivere =
    FlereArbeidsgivere(
        meldingTilArbeidsgiver = meldingTilArbeidsgiver,
        tiltakArbeidsplassen = tiltakArbeidsplassen,
        navn = navn,
        yrkesbetegnelse = yrkesbetegnelse,
        stillingsprosent = stillingsprosent,
    )

fun lagArbeidsgiverInfoIngenArbeidsgiver(): IngenArbeidsgiver = IngenArbeidsgiver()

fun lagPrognose(
    arbeidsforEtterPeriode: Boolean = false,
    hensynArbeidsplassen: String? = "Tilrettelegging på arbeidsplassen anbefales",
    arbeid: IArbeid? = null,
): Prognose =
    Prognose(
        arbeidsforEtterPeriode = arbeidsforEtterPeriode,
        hensynArbeidsplassen = hensynArbeidsplassen,
        arbeid = arbeid,
    )

fun lagTiltak(
    tiltakNav: String? = "Behov for tilrettelegging",
    andreTiltak: String? = "Redusert arbeidstid",
): Tiltak =
    Tiltak(
        tiltakNav = tiltakNav,
        andreTiltak = andreTiltak,
    )

fun lagBistandNav(
    bistandUmiddelbart: Boolean = false,
    beskrivBistand: String? = "Ingen behov for bistand per nå",
): BistandNav =
    BistandNav(
        bistandUmiddelbart = bistandUmiddelbart,
        beskrivBistand = beskrivBistand,
    )
