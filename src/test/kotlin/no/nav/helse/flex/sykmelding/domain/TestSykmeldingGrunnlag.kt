package no.nav.helse.flex.sykmelding.domain

import java.time.LocalDate
import java.time.OffsetDateTime

fun lagSykmeldingGrunnlag(id: String = "1"): SykmeldingGrunnlag {
    return SykmeldingGrunnlag(
        id = id,
        metadata = lagSykmeldingMetadata(),
        pasient = lagPasient(),
        medisinskVurdering = lagMedisinskVurdering(),
        aktivitet =
            listOf(
                lagAktivitetIkkeMulig(),
            ),
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
}

fun lagUtenlandskSykmeldingGrunnlag(): UtenlandskSykmeldingGrunnlag {
    return UtenlandskSykmeldingGrunnlag(
        id = "1",
        metadata = lagSykmeldingMetadata(),
        pasient = lagPasient(),
        medisinskVurdering = lagMedisinskVurdering(),
        aktivitet = listOf(lagAktivitetIkkeMulig()),
        utenlandskInfo =
            UtenlandskInfo(
                land = "Sverige",
                folkeRegistertAdresseErBrakkeEllerTilsvarende = false,
                erAdresseUtland = false,
            ),
    )
}

fun lagSykmeldingMetadata(): SykmeldingMetadata {
    return SykmeldingMetadata(
        mottattDato = OffsetDateTime.now(),
        genDate = OffsetDateTime.now().minusDays(1),
        behandletTidspunkt = OffsetDateTime.now().minusHours(2),
        regelsettVersjon = "1.0",
        avsenderSystem =
            AvsenderSystem(
                navn = "EPJSystem",
                versjon = "2.1.0",
            ),
        strekkode = "ABC12345",
    )
}

fun lagPasient(): Pasient {
    return Pasient(
        fnr = "01010112345",
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
}

fun lagMedisinskVurdering(): MedisinskVurdering {
    return MedisinskVurdering(
        hovedDiagnose =
            DiagnoseInfo(
                system = DiagnoseSystem.ICPC2,
                kode = "R51",
            ),
        biDiagnoser =
            listOf(
                DiagnoseInfo(
                    system = DiagnoseSystem.ICD10,
                    kode = "J06.9",
                ),
            ),
        svangerskap = false,
        annenFraversArsak = null,
        yrkesskade = null,
        skjermetForPasient = false,
        syketilfelletStartDato = null,
    )
}

fun lagAktivitetBehandlingsdager(): Behandlingsdager {
    return Behandlingsdager(
        antallBehandlingsdager = 1,
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
    )
}

fun lagAktivitetGradert(): Gradert {
    return Gradert(
        grad = 1,
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
        reisetilskudd = false,
    )
}

fun lagAktivitetReisetilskudd(): Reisetilskudd {
    return Reisetilskudd(
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
    )
}

fun lagAktivitetAvventende(): Avventende {
    return Avventende(
        innspillTilArbeidsgiver = "Ingen",
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
    )
}

fun lagAktivitetIkkeMulig() =
    AktivitetIkkeMulig(
        medisinskArsak =
            MedisinskArsak(
                arsak = MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET,
                beskrivelse = "Pasient er syk",
            ),
        arbeidsrelatertArsak = null,
        fom = LocalDate.now().minusDays(1),
        tom = LocalDate.now().plusDays(1),
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
