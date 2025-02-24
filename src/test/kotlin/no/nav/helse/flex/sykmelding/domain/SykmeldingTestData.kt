package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.api.dto.*
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

fun lagSykmeldingSporsmalSvarDto(
    arbeidsgiverOrgnummer: String = "123456789",
): _root_ide_package_.no.nav.helse.flex.api.dto.SykmeldingSporsmalSvarDto =
    _root_ide_package_.no.nav.helse.flex.api.dto.SykmeldingSporsmalSvarDto(
        erOpplysningeneRiktige =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Er opplysningene riktige?",
                svar = _root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei.JA,
            ),
        uriktigeOpplysninger =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Hvilke opplysninger er uriktige?",
                svar =
                    listOf(
                        _root_ide_package_.no.nav.helse.flex.api.dto.UriktigeOpplysningerType.PERIODE,
                        _root_ide_package_.no.nav.helse.flex.api.dto.UriktigeOpplysningerType.DIAGNOSE,
                    ),
            ),
        arbeidssituasjon =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Hva er din arbeidssituasjon?",
                svar = _root_ide_package_.no.nav.helse.flex.api.dto.Arbeidssituasjon.ARBEIDSTAKER,
            ),
        arbeidsgiverOrgnummer =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Hva er arbeidsgiverens orgnummer?",
                svar = arbeidsgiverOrgnummer,
            ),
        arbeidsledig =
            _root_ide_package_.no.nav.helse.flex.api.dto.ArbeidsledigFraOrgnummer(
                arbeidsledigFraOrgnummer =
                    _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                        sporsmaltekst = "Hva er orgnummeret du er arbeidsledig fra?",
                        svar = "987654321",
                    ),
            ),
        riktigNarmesteLeder =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Er dette riktig nærmeste leder?",
                svar = _root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei.JA,
            ),
        harBruktEgenmelding =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Har du brukt egenmelding?",
                svar = _root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei.NEI,
            ),
        egenmeldingsperioder =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Hvilke egenmeldingsperioder har du hatt?",
                svar =
                    listOf(
                        _root_ide_package_.no.nav.helse.flex.api.dto.Egenmeldingsperiode(
                            fom = LocalDate.parse("2023-01-01"),
                            tom = LocalDate.parse("2023-01-05"),
                        ),
                    ),
            ),
        harForsikring =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Har du forsikring?",
                svar = _root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei.JA,
            ),
        egenmeldingsdager =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Hvilke egenmeldingsdager har du hatt?",
                svar =
                    listOf(
                        LocalDate.parse("2023-01-02"),
                        LocalDate.parse("2023-01-03"),
                    ),
            ),
        harBruktEgenmeldingsdager =
            _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                sporsmaltekst = "Har du brukt egenmeldingsdager?",
                svar = _root_ide_package_.no.nav.helse.flex.api.dto.JaEllerNei.NEI,
            ),
        fisker =
            _root_ide_package_.no.nav.helse.flex.api.dto.FiskerSvar(
                blad =
                    _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                        sporsmaltekst = "Hvilket blad?",
                        svar = _root_ide_package_.no.nav.helse.flex.api.dto.Blad.A,
                    ),
                lottOgHyre =
                    _root_ide_package_.no.nav.helse.flex.api.dto.FormSporsmalSvar(
                        sporsmaltekst = "Lott og hyre?",
                        svar = _root_ide_package_.no.nav.helse.flex.api.dto.LottOgHyre.LOTT,
                    ),
            ),
    )
