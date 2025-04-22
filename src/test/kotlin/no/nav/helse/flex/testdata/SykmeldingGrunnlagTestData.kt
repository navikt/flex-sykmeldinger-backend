package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.domain.tsm.*
import no.nav.helse.flex.sykmelding.domain.tsm.values.*
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
): SykmeldingGrunnlag =
    SykmeldingGrunnlag(
        id = id,
        metadata = metadata,
        pasient = pasient,
        medisinskVurdering = lagMedisinskVurdering(),
        aktivitet = aktiviteter,
        behandler = lagBehandler(),
        arbeidsgiver =
            EnArbeidsgiver(
                meldingTilArbeidsgiver = "Melding til arbeidsgiver",
                tiltakArbeidsplassen = "Dette er et tiltak",
            ),
        signerendeBehandler =
            SignerendeBehandler(
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
                tiltakNAV = "Behov for tilrettelegging",
                andreTiltak = "Redusert arbeidstid",
            ),
        bistandNav =
            BistandNav(
                bistandUmiddelbart = false,
                beskrivBistand = "Ingen behov for bistand per nå",
            ),
        tilbakedatering =
            Tilbakedatering(
                kontaktDato = LocalDate.now().minusDays(5),
                begrunnelse = "Pasienten kunne ikke oppsøke lege tidligere",
            ),
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

fun lagUtenlandskSykmeldingGrunnlag(): UtenlandskSykmeldingGrunnlag =
    UtenlandskSykmeldingGrunnlag(
        id = "1",
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

fun lagSykmeldingMetadata(
    avsenderSystem: AvsenderSystem =
        AvsenderSystem(
            navn = "EPJSystem",
            versjon = "2.1.0",
        ),
): SykmeldingMetadata {
    val avsenderSystem = avsenderSystem
    return SykmeldingMetadata(
        mottattDato = OffsetDateTime.now(),
        genDate = OffsetDateTime.now().minusDays(1),
        behandletTidspunkt = OffsetDateTime.now().minusHours(2),
        regelsettVersjon = "1.0",
        avsenderSystem = avsenderSystem,
        strekkode = "ABC12345",
    )
}

fun lagPasient(fnr: String = "01010112345"): Pasient =
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
        navnFastlege = null,
    )

fun lagMedisinskVurdering(
    hovedDiagnoseKode: String = "R51",
    annenFraverArsak: AnnenFraverArsak? = null,
): MedisinskVurdering =
    MedisinskVurdering(
        hovedDiagnose =
            DiagnoseInfo(
                system = DiagnoseSystem.ICPC2,
                kode = hovedDiagnoseKode,
            ),
        biDiagnoser =
            listOf(
                DiagnoseInfo(
                    system = DiagnoseSystem.ICD10,
                    kode = "J06.9",
                ),
            ),
        svangerskap = false,
        annenFraversArsak = annenFraverArsak,
        yrkesskade = null,
        skjermetForPasient = false,
        syketilfelletStartDato = null,
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
            arsak = MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET,
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
