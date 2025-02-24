package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.api.dto.*
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun lagSykmelding(
    sykmeldingGrunnlag: ISykmeldingGrunnlag = lagSykmeldingGrunnlag(id = "1"),
    statuser: List<SykmeldingHendelse> =
        listOf(
            lagSykmeldingHendelse(),
        ),
): Sykmelding =
    Sykmelding(
        sykmeldingGrunnlag = sykmeldingGrunnlag,
        statuser = statuser,
        opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
        oppdatert = Instant.parse("2021-01-01T00:00:00.00Z"),
    )

fun lagSykmeldingHendelse(
    status: HendelseStatus = HendelseStatus.APEN,
    sporsmalSvar: List<Sporsmal>? = null,
    arbeidstakerInfo: ArbeidstakerInfo? = null,
) = SykmeldingHendelse(
    status = status,
    opprettet = Instant.parse("2021-01-01T00:00:00.00Z"),
    sporsmalSvar = sporsmalSvar,
    arbeidstakerInfo = arbeidstakerInfo,
)

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
): SykmeldingGrunnlag =
    SykmeldingGrunnlag(
        id = id,
        metadata = lagSykmeldingMetadata(),
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

fun lagSykmeldingMetadata(): SykmeldingMetadata =
    SykmeldingMetadata(
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

fun lagMedisinskVurdering(): MedisinskVurdering =
    MedisinskVurdering(
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

fun lagValidation(): ValidationResult =
    ValidationResult(
        status = RuleType.OK,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
        rules = listOf(),
    )

fun lagSykmeldingSporsmalSvarDto(arbeidsgiverOrgnummer: String = "123456789"): SykmeldingSporsmalSvarDto =
    SykmeldingSporsmalSvarDto(
        erOpplysningeneRiktige =
            FormSporsmalSvar(
                sporsmaltekst = "Er opplysningene riktige?",
                svar = JaEllerNei.JA,
            ),
        uriktigeOpplysninger =
            FormSporsmalSvar(
                sporsmaltekst = "Hvilke opplysninger er uriktige?",
                svar =
                    listOf(
                        UriktigeOpplysningerType.PERIODE,
                        UriktigeOpplysningerType.DIAGNOSE,
                    ),
            ),
        arbeidssituasjon =
            FormSporsmalSvar(
                sporsmaltekst = "Hva er din arbeidssituasjon?",
                svar = Arbeidssituasjon.ARBEIDSTAKER,
            ),
        arbeidsgiverOrgnummer =
            FormSporsmalSvar(
                sporsmaltekst = "Hva er arbeidsgiverens orgnummer?",
                svar = arbeidsgiverOrgnummer,
            ),
        arbeidsledig =
            ArbeidsledigFraOrgnummer(
                arbeidsledigFraOrgnummer =
                    FormSporsmalSvar(
                        sporsmaltekst = "Hva er orgnummeret du er arbeidsledig fra?",
                        svar = "987654321",
                    ),
            ),
        riktigNarmesteLeder =
            FormSporsmalSvar(
                sporsmaltekst = "Er dette riktig nærmeste leder?",
                svar = JaEllerNei.JA,
            ),
        harBruktEgenmelding =
            FormSporsmalSvar(
                sporsmaltekst = "Har du brukt egenmelding?",
                svar = JaEllerNei.NEI,
            ),
        egenmeldingsperioder =
            FormSporsmalSvar(
                sporsmaltekst = "Hvilke egenmeldingsperioder har du hatt?",
                svar =
                    listOf(
                        Egenmeldingsperiode(
                            fom = LocalDate.parse("2023-01-01"),
                            tom = LocalDate.parse("2023-01-05"),
                        ),
                    ),
            ),
        harForsikring =
            FormSporsmalSvar(
                sporsmaltekst = "Har du forsikring?",
                svar = JaEllerNei.JA,
            ),
        egenmeldingsdager =
            FormSporsmalSvar(
                sporsmaltekst = "Hvilke egenmeldingsdager har du hatt?",
                svar =
                    listOf(
                        LocalDate.parse("2023-01-02"),
                        LocalDate.parse("2023-01-03"),
                    ),
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
