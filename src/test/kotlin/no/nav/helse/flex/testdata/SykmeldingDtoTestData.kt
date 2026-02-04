package no.nav.helse.flex.testdata

import no.nav.helse.flex.api.dto.*
import java.time.LocalDate
import java.time.OffsetDateTime

fun lagSykmeldingDto(
    id: String = "default-test-id",
    pasient: PasientDTO = PasientDTO(fnr = "12345678901", fornavn = "Ola", mellomnavn = null, etternavn = "Nordmann", overSyttiAar = false),
    mottattTidspunkt: OffsetDateTime = OffsetDateTime.now(),
    behandlingsutfall: BehandlingsutfallDTO =
        BehandlingsutfallDTO(
            status = RegelStatusDTO.OK,
            ruleHits = emptyList(),
            erUnderBehandling = false,
        ),
    legekontorOrgnummer: String? = "123456789",
    arbeidsgiver: ArbeidsgiverDTO? = ArbeidsgiverDTO(navn = "Bedrift AS", stillingsprosent = 100),
    sykmeldingsperioder: List<SykmeldingsperiodeDTO> =
        listOf(
            SykmeldingsperiodeDTO(
                fom = LocalDate.now(),
                tom = LocalDate.now().plusDays(10),
                gradert = null,
                behandlingsdager = null,
                innspillTilArbeidsgiver = null,
                type = PeriodetypeDTO.AKTIVITET_IKKE_MULIG,
                aktivitetIkkeMulig = null,
                reisetilskudd = false,
            ),
        ),
    sykmeldingStatus: SykmeldingStatusDTO =
        SykmeldingStatusDTO(
            statusEvent = "APEN",
            timestamp = OffsetDateTime.now(),
            arbeidsgiver = null,
            brukerSvar = null,
        ),
    medisinskVurdering: MedisinskVurderingDTO? = null,
    skjermesForPasient: Boolean = false,
    prognose: PrognoseDTO? = null,
    utdypendeOpplysninger: Map<String, Map<String, SporsmalSvarDTO>> = emptyMap(),
    tiltakArbeidsplassen: String? = null,
    tiltakNAV: String? = null,
    andreTiltak: String? = null,
    meldingTilNAV: MeldingTilNavDTO? = null,
    meldingTilArbeidsgiver: String? = null,
    kontaktMedPasient: KontaktMedPasientDTO? = null,
    behandletTidspunkt: OffsetDateTime = OffsetDateTime.now(),
    behandler: BehandlerDTO? = null,
    syketilfelleStartDato: LocalDate? = null,
    navnFastlege: String? = null,
    egenmeldt: Boolean? = null,
    papirsykmelding: Boolean? = null,
    harRedusertArbeidsgiverperiode: Boolean? = null,
    merknader: List<MerknadDTO>? = null,
    rulesetVersion: String? = null,
    utenlandskSykmelding: UtenlandskSykmelding? = null,
): SykmeldingDTO =
    SykmeldingDTO(
        id = id,
        pasient = pasient,
        mottattTidspunkt = mottattTidspunkt,
        behandlingsutfall = behandlingsutfall,
        legekontorOrgnummer = legekontorOrgnummer,
        arbeidsgiver = arbeidsgiver,
        sykmeldingsperioder = sykmeldingsperioder,
        sykmeldingStatus = sykmeldingStatus,
        medisinskVurdering = medisinskVurdering,
        skjermesForPasient = skjermesForPasient,
        prognose = prognose,
        utdypendeOpplysninger = utdypendeOpplysninger,
        tiltakArbeidsplassen = tiltakArbeidsplassen,
        tiltakNAV = tiltakNAV,
        andreTiltak = andreTiltak,
        meldingTilNAV = meldingTilNAV,
        meldingTilArbeidsgiver = meldingTilArbeidsgiver,
        kontaktMedPasient = kontaktMedPasient,
        behandletTidspunkt = behandletTidspunkt,
        behandler = behandler,
        syketilfelleStartDato = syketilfelleStartDato,
        navnFastlege = navnFastlege,
        egenmeldt = egenmeldt,
        papirsykmelding = papirsykmelding,
        harRedusertArbeidsgiverperiode = harRedusertArbeidsgiverperiode,
        merknader = merknader,
        rulesetVersion = rulesetVersion,
        utenlandskSykmelding = utenlandskSykmelding,
    )
