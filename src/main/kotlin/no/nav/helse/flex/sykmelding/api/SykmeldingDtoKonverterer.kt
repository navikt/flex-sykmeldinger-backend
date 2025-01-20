import no.nav.helse.flex.sykmelding.api.dto.AdresseDTO
import no.nav.helse.flex.sykmelding.api.dto.AktivitetIkkeMuligDTO
import no.nav.helse.flex.sykmelding.api.dto.AnnenFraverGrunnDTO
import no.nav.helse.flex.sykmelding.api.dto.AnnenFraversArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsgiverDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsrelatertArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsrelatertArsakTypeDTO
import no.nav.helse.flex.sykmelding.api.dto.BehandlerDTO
import no.nav.helse.flex.sykmelding.api.dto.BehandlingsutfallDTO
import no.nav.helse.flex.sykmelding.api.dto.DiagnoseDTO
import no.nav.helse.flex.sykmelding.api.dto.ErIArbeidDTO
import no.nav.helse.flex.sykmelding.api.dto.ErIkkeIArbeidDTO
import no.nav.helse.flex.sykmelding.api.dto.KontaktMedPasientDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskArsakDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskArsakTypeDTO
import no.nav.helse.flex.sykmelding.api.dto.MedisinskVurderingDTO
import no.nav.helse.flex.sykmelding.api.dto.MeldingTilNavDTO
import no.nav.helse.flex.sykmelding.api.dto.PasientDTO
import no.nav.helse.flex.sykmelding.api.dto.PeriodetypeDTO
import no.nav.helse.flex.sykmelding.api.dto.PrognoseDTO
import no.nav.helse.flex.sykmelding.api.dto.RegelStatusDTO
import no.nav.helse.flex.sykmelding.api.dto.SporsmalSvarDTO
import no.nav.helse.flex.sykmelding.api.dto.SvarRestriksjonDTO
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingDTO
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingStatusDTO
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingsperiodeDTO
import no.nav.helse.flex.sykmelding.domain.Adresse
import no.nav.helse.flex.sykmelding.domain.Aktivitet
import no.nav.helse.flex.sykmelding.domain.AktivitetIkkeMulig
import no.nav.helse.flex.sykmelding.domain.AktivitetType
import no.nav.helse.flex.sykmelding.domain.AnnenFravarArsakType
import no.nav.helse.flex.sykmelding.domain.AnnenFraverArsak
import no.nav.helse.flex.sykmelding.domain.ArbeidsgiverInfo
import no.nav.helse.flex.sykmelding.domain.ArbeidsrelatertArsakType
import no.nav.helse.flex.sykmelding.domain.Behandler
import no.nav.helse.flex.sykmelding.domain.BistandNav
import no.nav.helse.flex.sykmelding.domain.DiagnoseInfo
import no.nav.helse.flex.sykmelding.domain.EnArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.ErIArbeid
import no.nav.helse.flex.sykmelding.domain.ErIkkeIArbeid
import no.nav.helse.flex.sykmelding.domain.FlereArbeidsgivere
import no.nav.helse.flex.sykmelding.domain.Gradert
import no.nav.helse.flex.sykmelding.domain.IngenArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.KontaktinfoType
import no.nav.helse.flex.sykmelding.domain.MedisinskArsakType
import no.nav.helse.flex.sykmelding.domain.MedisinskVurdering
import no.nav.helse.flex.sykmelding.domain.Pasient
import no.nav.helse.flex.sykmelding.domain.Prognose
import no.nav.helse.flex.sykmelding.domain.SporsmalSvar
import no.nav.helse.flex.sykmelding.domain.SvarRestriksjon
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.SykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.domain.SykmeldingStatus
import no.nav.helse.flex.sykmelding.domain.Tilbakedatering
import no.nav.helse.flex.sykmelding.domain.UtenlandskSykmeldingGrunnlag
import java.time.ZoneOffset

class SykmeldingDtoKonverterer {
    fun konverter(sykmelding: Sykmelding): SykmeldingDTO =
        SykmeldingDTO(
            id = sykmelding.sykmeldingId,
            pasient = konverterPasient(sykmelding.sykmeldingGrunnlag.pasient),
            mottattTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.mottattDato,
            behandlingsutfall = konverterBehandlingsutfall(sykmelding),
            legekontorOrgnummer = "", // TODO: sykmelding.sykmeldingGrunnlag.metadata.avsenderSystem.navn,
            arbeidsgiver =
                when (sykmelding.sykmeldingGrunnlag) {
                    is SykmeldingGrunnlag -> konverterArbeidsgiver(sykmelding.sykmeldingGrunnlag.arbeidsgiver)
                    is UtenlandskSykmeldingGrunnlag -> null
                },
            sykmeldingsperioder = sykmelding.sykmeldingGrunnlag.aktivitet.map { konverterSykmeldingsperiode(it) },
            sykmeldingStatus = konverterSykmeldingStatus(sykmelding.sisteStatus()),
            medisinskVurdering = konverterMedisinskVurdering(sykmelding.sykmeldingGrunnlag.medisinskVurdering),
            skjermesForPasient = sykmelding.sykmeldingGrunnlag.medisinskVurdering.skjermetForPasient,
            prognose =
                when (sykmelding.sykmeldingGrunnlag) {
                    is SykmeldingGrunnlag -> sykmelding.sykmeldingGrunnlag.prognose?.let { konverterPrognose(it) }
                    is UtenlandskSykmeldingGrunnlag -> TODO()
                },
            utdypendeOpplysninger = konverterUtdypendeOpplysninger(sykmelding.sykmeldingGrunnlag.utdypendeOpplysninger),
            tiltakArbeidsplassen = TODO(),
            tiltakNAV = sykmelding.sykmeldingGrunnlag.tiltak?.tiltakNAV,
            andreTiltak = sykmelding.sykmeldingGrunnlag.tiltak?.andreTiltak,
            meldingTilNAV = sykmelding.sykmeldingGrunnlag.bistandNav?.let { konverterMeldingTilNAV(it) },
            meldingTilArbeidsgiver =
                when (sykmelding.sykmeldingGrunnlag.arbeidsgiver) {
                    is EnArbeidsgiver -> sykmelding.sykmeldingGrunnlag.arbeidsgiver.meldingTilArbeidsgiver
                    is FlereArbeidsgivere -> sykmelding.sykmeldingGrunnlag.arbeidsgiver.meldingTilArbeidsgiver
                    is IngenArbeidsgiver -> null
                },
            kontaktMedPasient = konverterKontaktMedPasient(sykmelding.sykmeldingGrunnlag.tilbakedatering),
            behandletTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.behandletTidspunkt,
            behandler = konverterBehandler(sykmelding.sykmeldingGrunnlag.behandler),
            syketilfelleStartDato =
                sykmelding.sykmeldingGrunnlag.metadata.genDate
                    .toLocalDate(),
            navnFastlege = null, // No input data available
            egenmeldt = null, // No input data available
            papirsykmelding = false, // Assuming false unless specified
            harRedusertArbeidsgiverperiode = null, // No input data available
            merknader = null, // No clear mapping provided
            rulesetVersion = sykmelding.sykmeldingGrunnlag.metadata.regelsettVersjon,
            utenlandskSykmelding = null, // Not provided in current mappings
        )

    internal fun konverterPasient(pasient: Pasient): PasientDTO =
        PasientDTO(
            fnr = pasient.fnr,
            fornavn = pasient.navn?.fornavn,
            mellomnavn = pasient.navn?.mellomnavn,
            etternavn = pasient.navn?.etternavn,
            overSyttiAar = null, // TODO: Data not available
        )

    internal fun konverterBehandlingsutfall(sykmelding: Sykmelding): BehandlingsutfallDTO =
        BehandlingsutfallDTO( // TODO: benytt behandlingsutfall fra tsm kafka melding
            status = RegelStatusDTO.OK, // Assuming OK, adjust based on domain logic
            ruleHits = emptyList(), // No rule hits mapping provided
        )

    internal fun konverterSykmeldingsperiode(aktivitet: Aktivitet): SykmeldingsperiodeDTO {
        val aktivitetIkkeMuligDto =
            if (aktivitet is AktivitetIkkeMulig) {
                val medisinskArsakDto =
                    if (aktivitet.medisinskArsak != null) {
                        MedisinskArsakDTO(
                            beskrivelse = aktivitet.medisinskArsak.beskrivelse,
                            arsak =
                                listOf(
                                    when (aktivitet.medisinskArsak.arsak) {
                                        MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET ->
                                            MedisinskArsakTypeDTO.TILSTAND_HINDRER_AKTIVITET
                                        MedisinskArsakType.AKTIVITET_FORVERRER_TILSTAND ->
                                            MedisinskArsakTypeDTO.AKTIVITET_FORVERRER_TILSTAND
                                        MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING ->
                                            MedisinskArsakTypeDTO.AKTIVITET_FORHINDRER_BEDRING
                                        MedisinskArsakType.ANNET -> MedisinskArsakTypeDTO.ANNET
                                    },
                                ),
                        )
                    } else {
                        null
                    }
                val arbeidsrelatertArsakDto =
                    if (aktivitet.arbeidsrelatertArsak != null) {
                        ArbeidsrelatertArsakDTO(
                            beskrivelse = aktivitet.arbeidsrelatertArsak.beskrivelse,
                            arsak =
                                listOf(
                                    when (aktivitet.arbeidsrelatertArsak.arsak) {
                                        ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING -> ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING
                                        ArbeidsrelatertArsakType.ANNET -> ArbeidsrelatertArsakTypeDTO.ANNET
                                    },
                                ),
                        )
                    } else {
                        null
                    }
                AktivitetIkkeMuligDTO(
                    medisinskArsak = medisinskArsakDto,
                    arbeidsrelatertArsak = arbeidsrelatertArsakDto,
                )
            } else {
                null
            }
        return SykmeldingsperiodeDTO(
            fom = aktivitet.fom,
            tom = aktivitet.tom,
            type =
                when (aktivitet.type) {
                    AktivitetType.AKTIVITET_IKKE_MULIG -> PeriodetypeDTO.AKTIVITET_IKKE_MULIG
                    AktivitetType.AVVENTENDE -> PeriodetypeDTO.AVVENTENDE
                    AktivitetType.BEHANDLINGSDAGER -> PeriodetypeDTO.BEHANDLINGSDAGER
                    AktivitetType.GRADERT -> PeriodetypeDTO.GRADERT
                    AktivitetType.REISETILSKUDD -> PeriodetypeDTO.REISETILSKUDD
                },
            aktivitetIkkeMulig = aktivitetIkkeMuligDto,
            gradert =
                when (aktivitet) {
                    is Gradert -> TODO()
                    else -> TODO()
                },
            // Data not available
            behandlingsdager = null, // Data not available
            reisetilskudd = false, // Default assumption
            innspillTilArbeidsgiver = null, // Data not available
        )
    }

    internal fun konverterMedisinskVurdering(medisinskVurdering: MedisinskVurdering): MedisinskVurderingDTO =
        MedisinskVurderingDTO(
            biDiagnoser = medisinskVurdering.biDiagnoser?.map { konverterDiagnose(it) } ?: emptyList(),
            yrkesskade = medisinskVurdering.yrkesskade != null,
            yrkesskadeDato = medisinskVurdering.yrkesskade?.yrkesskadeDato,
            annenFraversArsak = medisinskVurdering.annenFraversArsak?.let { konverterAnnenFraversArsak(it) },
            svangerskap = medisinskVurdering.svangerskap,
            hovedDiagnose = medisinskVurdering.hovedDiagnose?.let { konverterDiagnose(it) },
        )

    internal fun konverterAnnenFraversArsak(annenFraverArsak: AnnenFraverArsak): AnnenFraversArsakDTO? =
        AnnenFraversArsakDTO(
            beskrivelse = annenFraverArsak.beskrivelse,
            grunn =
                annenFraverArsak.arsak?.map {
                    when (it) {
                        AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON -> AnnenFraverGrunnDTO.GODKJENT_HELSEINSTITUSJON
                        AnnenFravarArsakType.BEHANDLING_FORHINDRER_ARBEID -> AnnenFraverGrunnDTO.BEHANDLING_FORHINDRER_ARBEID
                        AnnenFravarArsakType.ARBEIDSRETTET_TILTAK -> AnnenFraverGrunnDTO.ARBEIDSRETTET_TILTAK
                        AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND -> AnnenFraverGrunnDTO.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND
                        AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE -> AnnenFraverGrunnDTO.NODVENDIG_KONTROLLUNDENRSOKELSE
                        AnnenFravarArsakType.SMITTEFARE -> AnnenFraverGrunnDTO.SMITTEFARE
                        AnnenFravarArsakType.ABORT -> AnnenFraverGrunnDTO.ABORT
                        AnnenFravarArsakType.UFOR_GRUNNET_BARNLOSHET -> AnnenFraverGrunnDTO.UFOR_GRUNNET_BARNLOSHET
                        AnnenFravarArsakType.DONOR -> AnnenFraverGrunnDTO.DONOR
                        AnnenFravarArsakType.BEHANDLING_STERILISERING -> AnnenFraverGrunnDTO.BEHANDLING_STERILISERING
                    }
                } ?: emptyList(),
        )

    internal fun konverterDiagnose(diagnose: DiagnoseInfo): DiagnoseDTO =
        DiagnoseDTO(
            tekst = TODO(),
            system = diagnose.system.name,
            kode = diagnose.kode,
        )

    internal fun konverterPrognose(prognose: Prognose): PrognoseDTO {
        val erIArbeidDTO =
            (prognose.arbeid as? ErIArbeid)?.let {
                ErIArbeidDTO(
                    egetArbeidPaSikt = it.egetArbeidPaSikt,
                    annetArbeidPaSikt = it.annetArbeidPaSikt,
                    arbeidFOM = it.arbeidFOM,
                    vurderingsdato = it.vurderingsdato,
                )
            }

        val erIkkeIArbeidDTO =
            (prognose.arbeid as? ErIkkeIArbeid)?.let {
                ErIkkeIArbeidDTO(
                    arbeidsforPaSikt = it.arbeidsforPaSikt,
                    arbeidsforFOM = it.arbeidsforFOM,
                    vurderingsdato = it.vurderingsdato,
                )
            }

        return PrognoseDTO(
            arbeidsforEtterPeriode = prognose.arbeidsforEtterPeriode,
            hensynArbeidsplassen = prognose.hensynArbeidsplassen,
            erIArbeid = erIArbeidDTO,
            erIkkeIArbeid = erIkkeIArbeidDTO,
        )
    }

    internal fun konverterMeldingTilNAV(bistandNav: BistandNav): MeldingTilNavDTO =
        MeldingTilNavDTO(
            bistandUmiddelbart = bistandNav.bistandUmiddelbart,
            beskrivBistand = bistandNav.beskrivBistand,
        )

    internal fun konverterKontaktMedPasient(tilbakedatering: Tilbakedatering?): KontaktMedPasientDTO =
        KontaktMedPasientDTO(
            begrunnelseIkkeKontakt = tilbakedatering?.begrunnelse,
            kontaktDato = tilbakedatering?.kontaktDato,
        )

    internal fun konverterBehandler(behandler: Behandler): BehandlerDTO =
        BehandlerDTO(
            fornavn = behandler.navn.fornavn,
            mellomnavn = behandler.navn.mellomnavn,
            etternavn = behandler.navn.etternavn,
            tlf = behandler.kontaktinfo.find { it.type == KontaktinfoType.TLF }?.value,
            adresse = konverterAdresse(behandler.adresse),
        )

    internal fun konverterAdresse(adresse: Adresse?): AdresseDTO {
        if (adresse == null) return AdresseDTO(null, null, null, null, null)
        return AdresseDTO(
            postnummer = adresse.postnummer?.toIntOrNull(),
            postboks = adresse.postboks,
            kommune = adresse.kommune,
            gate = adresse.gateadresse,
            land = adresse.land,
        )
    }

    internal fun konverterSykmeldingStatus(status: SykmeldingStatus): SykmeldingStatusDTO =
        SykmeldingStatusDTO(
            statusEvent = status.status,
            timestamp = status.opprettet.atOffset(ZoneOffset.UTC),
            sporsmalOgSvarListe = emptyList(), // No mapping provided
            arbeidsgiver = null, // No mapping provided
            brukerSvar = null, // No mapping provided
        )

    internal fun konverterArbeidsgiver(arbeidsgiverInfo: ArbeidsgiverInfo): ArbeidsgiverDTO? =
        when (arbeidsgiverInfo) {
            is FlereArbeidsgivere ->
                ArbeidsgiverDTO(
                    navn = arbeidsgiverInfo.navn,
                    stillingsprosent = arbeidsgiverInfo.stillingsprosent,
                )
            is EnArbeidsgiver -> null
            is IngenArbeidsgiver -> null
        }

    internal fun konverterUtdypendeOpplysninger(
        utdypendeOpplysninger: Map<String, Map<String, SporsmalSvar>>?,
    ): Map<String, Map<String, SporsmalSvarDTO>> =
        utdypendeOpplysninger?.mapValues { (_, innerMap) ->
            innerMap.mapValues { (_, sporsmalSvar) ->
                konverterSporsmalSvar(sporsmalSvar)
            }
        } ?: emptyMap()

    internal fun konverterSporsmalSvar(sporsmalSvar: SporsmalSvar): SporsmalSvarDTO =
        SporsmalSvarDTO(
            sporsmal = sporsmalSvar.sporsmal,
            svar = sporsmalSvar.svar,
            restriksjoner = sporsmalSvar.restriksjoner.map { konverterSvarRestriksjon(it) },
        )

    internal fun konverterSvarRestriksjon(restriksjon: SvarRestriksjon): SvarRestriksjonDTO =
        when (restriksjon) {
            SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER ->
                SvarRestriksjonDTO.SKJERMET_FOR_ARBEIDSGIVER
            SvarRestriksjon.SKJERMET_FOR_PASIENT ->
                SvarRestriksjonDTO.SKJERMET_FOR_PASIENT
            SvarRestriksjon.SKJERMET_FOR_NAV ->
                SvarRestriksjonDTO.SKJERMET_FOR_NAV
        }
}
