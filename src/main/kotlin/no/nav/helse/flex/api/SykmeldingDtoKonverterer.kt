package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.*
import no.nav.helse.flex.sykmelding.domain.tsm.SporsmalSvar
import no.nav.helse.flex.sykmelding.domain.tsm.values.Adresse
import no.nav.helse.flex.sykmelding.domain.tsm.values.Behandler
import no.nav.helse.flex.sykmelding.domain.tsm.values.KontaktinfoType
import no.nav.helse.flex.sykmelding.domain.tsm.values.Pasient
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class SykmeldingDtoKonverterer(
    private val sykmeldingStatusDtoKonverterer: SykmeldingStatusDtoKonverterer,
    private val sykmeldingRegelAvklaringer: SykmeldingRegelAvklaringer,
) {
    val log = logger()

    fun konverter(sykmelding: Sykmelding): SykmeldingDTO =
        when (sykmelding.sykmeldingGrunnlag) {
            is NorskSykmeldingGrunnlag -> konverterSykmelding(sykmelding)
            is UtenlandskSykmeldingGrunnlag -> konverterUtenlandskSykmelding(sykmelding)
        }

    internal fun konverterSykmelding(sykmelding: Sykmelding): SykmeldingDTO {
        if (sykmelding.sykmeldingGrunnlag !is NorskSykmeldingGrunnlag) {
            throw IllegalArgumentException("SykmeldingGrunnlag er ikke av type SykmeldingGrunnlag")
        }
        val sykmeldingsperioder = sykmelding.sykmeldingGrunnlag.aktivitet.map { konverterSykmeldingsperiode(it) }
        val medisinskVurdering = konverterMedisinskVurdering(sykmelding.sykmeldingGrunnlag.medisinskVurdering)
        val arbeidsgiver = sykmelding.sykmeldingGrunnlag.arbeidsgiver

        return SykmeldingDTO(
            id = sykmelding.sykmeldingId,
            pasient =
                konverterPasient(
                    pasient = sykmelding.sykmeldingGrunnlag.pasient,
                    fom = sykmeldingsperioder.minBy { it.fom }.fom,
                ),
            mottattTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.mottattDato,
            behandlingsutfall = konverterBehandlingsutfall(sykmelding.validation),
            arbeidsgiver = arbeidsgiver.tilArbeidsgiverDTO(),
            sykmeldingsperioder = sykmeldingsperioder,
            sykmeldingStatus = sykmeldingStatusDtoKonverterer.konverterSykmeldingStatus(sykmelding.sisteHendelse()),
            medisinskVurdering = medisinskVurdering,
            skjermesForPasient = sykmelding.sykmeldingGrunnlag.medisinskVurdering.skjermetForPasient,
            prognose = sykmelding.sykmeldingGrunnlag.prognose?.let { konverterPrognose(it) },
            utdypendeOpplysninger = konverterUtdypendeOpplysninger(sykmelding.sykmeldingGrunnlag.utdypendeOpplysninger),
            tiltakArbeidsplassen = arbeidsgiver.getTiltakArbeidsplassen(),
            tiltakNAV = sykmelding.sykmeldingGrunnlag.tiltak?.tiltakNav,
            andreTiltak = sykmelding.sykmeldingGrunnlag.tiltak?.andreTiltak,
            meldingTilNAV = sykmelding.sykmeldingGrunnlag.bistandNav?.let { konverterMeldingTilNAV(it) },
            meldingTilArbeidsgiver = arbeidsgiver.getMeldingTilArbeidsgiver(),
            kontaktMedPasient = sykmelding.sykmeldingGrunnlag.tilbakedatering?.let { konverterKontaktMedPasient(it) },
            behandletTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.behandletTidspunkt,
            behandler = konverterBehandler(sykmelding.sykmeldingGrunnlag.behandler),
            syketilfelleStartDato = sykmelding.sykmeldingGrunnlag.medisinskVurdering.syketilfelletStartDato,
            navnFastlege = sykmelding.sykmeldingGrunnlag.pasient.navnFastlege,
            egenmeldt = sykmelding.avsenderSystemNavn == AvsenderSystemNavn.EGENMELDT,
            papirsykmelding = sykmelding.avsenderSystemNavn == AvsenderSystemNavn.PAPIRSYKMELDING,
            harRedusertArbeidsgiverperiode =
                sykmeldingRegelAvklaringer.harRedusertArbeidsgiverperiode(
                    hovedDiagnose = medisinskVurdering.hovedDiagnose,
                    biDiagnoser = medisinskVurdering.biDiagnoser,
                    sykmeldingsperioder = sykmeldingsperioder,
                    annenFraversArsakDTO = medisinskVurdering.annenFraversArsak,
                ),
            merknader = konverterMerknader(sykmelding.validation),
            rulesetVersion = sykmelding.sykmeldingGrunnlag.metadata.regelsettVersjon,
            legekontorOrgnummer = null,
            utenlandskSykmelding = null,
        )
    }

    internal fun konverterUtenlandskSykmelding(sykmelding: Sykmelding): SykmeldingDTO {
        require(sykmelding.sykmeldingGrunnlag is UtenlandskSykmeldingGrunnlag)
        val sykmeldingsperioder = sykmelding.sykmeldingGrunnlag.aktivitet.map { konverterSykmeldingsperiode(it) }
        val medisinskVurdering = konverterMedisinskVurdering(sykmelding.sykmeldingGrunnlag.medisinskVurdering)

        return SykmeldingDTO(
            id = sykmelding.sykmeldingId,
            pasient =
                konverterPasient(
                    pasient = sykmelding.sykmeldingGrunnlag.pasient,
                    fom = sykmeldingsperioder.minBy { it.fom }.fom,
                ),
            mottattTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.mottattDato,
            behandlingsutfall = konverterBehandlingsutfall(sykmelding.validation),
            sykmeldingsperioder = sykmeldingsperioder,
            sykmeldingStatus = sykmeldingStatusDtoKonverterer.konverterSykmeldingStatus(sykmelding.sisteHendelse()),
            medisinskVurdering = medisinskVurdering,
            skjermesForPasient = sykmelding.sykmeldingGrunnlag.medisinskVurdering.skjermetForPasient,
            behandletTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.behandletTidspunkt,
            syketilfelleStartDato = sykmelding.sykmeldingGrunnlag.medisinskVurdering.syketilfelletStartDato,
            navnFastlege = sykmelding.sykmeldingGrunnlag.pasient.navnFastlege,
            egenmeldt = sykmelding.avsenderSystemNavn == AvsenderSystemNavn.EGENMELDT,
            papirsykmelding = sykmelding.avsenderSystemNavn == AvsenderSystemNavn.PAPIRSYKMELDING,
            harRedusertArbeidsgiverperiode =
                sykmeldingRegelAvklaringer.harRedusertArbeidsgiverperiode(
                    hovedDiagnose = medisinskVurdering.hovedDiagnose,
                    biDiagnoser = medisinskVurdering.biDiagnoser,
                    sykmeldingsperioder = sykmeldingsperioder,
                    annenFraversArsakDTO = medisinskVurdering.annenFraversArsak,
                ),
            utenlandskSykmelding =
                UtenlandskSykmelding(
                    land = sykmelding.sykmeldingGrunnlag.utenlandskInfo.land,
                ),
            rulesetVersion = sykmelding.sykmeldingGrunnlag.metadata.regelsettVersjon,
            merknader = konverterMerknader(sykmelding.validation),
            legekontorOrgnummer = null,
            arbeidsgiver = null,
            kontaktMedPasient = null,
            utdypendeOpplysninger = emptyMap(),
            tiltakArbeidsplassen = null,
            tiltakNAV = null,
            andreTiltak = null,
            meldingTilNAV = null,
            meldingTilArbeidsgiver = null,
            prognose = null,
            behandler = null,
        )
    }

    internal fun konverterMerknader(validationResult: ValidationResult): List<MerknadDTO> =
        validationResult.rules
            .map {
                MerknadDTO(
                    type =
                        when (it.name) {
                            "DELVIS_GODKJENT" -> MerknadtypeDTO.DELVIS_GODKJENT
                            "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER" ->
                                MerknadtypeDTO.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER
                            "UGYLDIG_TILBAKEDATERING" -> MerknadtypeDTO.UGYLDIG_TILBAKEDATERING
                            "UNDER_BEHANDLING" -> MerknadtypeDTO.UNDER_BEHANDLING
                            else -> MerknadtypeDTO.UKJENT_MERKNAD
                        },
                    beskrivelse = it.description,
                )
            }

    internal fun konverterPasient(
        pasient: Pasient,
        fom: LocalDate,
    ): PasientDTO =
        PasientDTO(
            fnr = pasient.fnr,
            fornavn = pasient.navn?.fornavn,
            mellomnavn = pasient.navn?.mellomnavn,
            etternavn = pasient.navn?.etternavn,
            overSyttiAar = sykmeldingRegelAvklaringer.erOverSyttiAar(pasientFnr = pasient.fnr, fom = fom),
        )

    internal fun konverterBehandlingsutfall(validationResult: ValidationResult): BehandlingsutfallDTO =
        when (validationResult.status) {
            RuleType.OK -> {
                BehandlingsutfallDTO(
                    status = RegelStatusDTO.OK,
                    ruleHits = emptyList(),
                )
            }
            RuleType.PENDING -> {
                BehandlingsutfallDTO(
                    status = RegelStatusDTO.OK,
                    ruleHits = emptyList(),
                )
            }
            RuleType.INVALID -> {
                val ruleHits =
                    validationResult.rules.mapNotNull { rule ->
                        if (rule is InvalidRule) {
                            RegelinfoDTO(
                                messageForSender = rule.reason?.sykmelder ?: "",
                                messageForUser = rule.reason?.sykmeldt ?: "",
                                ruleName =
                                    when (rule.name) {
                                        "BEHANDLER_IKKE_GYLDIG_I_HPR" ->
                                            RuleNameDTO.BEHANDLER_IKKE_GYLDIG_I_HPR.name
                                        "BEHANDLER_MANGLER_AUTORISASJON_I_HPR" ->
                                            RuleNameDTO.BEHANDLER_MANGLER_AUTORISASJON_I_HPR.name
                                        "BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR" ->
                                            RuleNameDTO.BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR.name
                                        "BEHANDLER_MT_FT_KI_OVER_12_UKER" ->
                                            RuleNameDTO.BEHANDLER_MT_FT_KI_OVER_12_UKER.name
                                        "BEHANDLER_SUSPENDERT" ->
                                            RuleNameDTO.BEHANDLER_SUSPENDERT.name
                                        "PASIENT_ELDRE_ENN_70" ->
                                            RuleNameDTO.PASIENT_ELDRE_ENN_70.name
                                        "ICPC_2_Z_DIAGNOSE" ->
                                            RuleNameDTO.ICPC_2_Z_DIAGNOSE.name
                                        "GRADERT_UNDER_20_PROSENT" ->
                                            RuleNameDTO.GRADERT_UNDER_20_PROSENT.name
                                        else -> {
                                            log.warn("Ukjent regelnavn ${rule.name} for behandlingsutfall: ${rule.serialisertTilString()}")
                                            rule.name
                                        }
                                    },
                                ruleStatus = RegelStatusDTO.INVALID,
                            )
                        } else {
                            null
                        }
                    }

                BehandlingsutfallDTO(
                    status = RegelStatusDTO.INVALID,
                    ruleHits = ruleHits,
                )
            }
        }

    internal fun konverterSykmeldingsperiode(aktivitet: Aktivitet): SykmeldingsperiodeDTO {
        val aktivitetIkkeMuligDto =
            if (aktivitet is AktivitetIkkeMulig) {
                val medisinskArsakDto =
                    aktivitet.medisinskArsak.map {
                        MedisinskArsakDTO(
                            beskrivelse = it.beskrivelse,
                            arsak =
                                listOf(
                                    when (it.arsak) {
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
                    }
                val arbeidsrelatertArsakDto =
                    aktivitet.arbeidsrelatertArsak.map {
                        ArbeidsrelatertArsakDTO(
                            beskrivelse = it.beskrivelse,
                            arsak =
                                listOf(
                                    when (it.arsak) {
                                        ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING ->
                                            ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING

                                        ArbeidsrelatertArsakType.ANNET -> ArbeidsrelatertArsakTypeDTO.ANNET
                                    },
                                ),
                        )
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
                    is Gradert ->
                        GradertDTO(
                            grad = aktivitet.grad,
                            reisetilskudd = aktivitet.reisetilskudd,
                        )
                    else -> null
                },
            behandlingsdager =
                when (aktivitet) {
                    is Behandlingsdager -> aktivitet.antallBehandlingsdager
                    else -> null
                },
            reisetilskudd = aktivitet is Reisetilskudd,
            innspillTilArbeidsgiver =
                when (aktivitet) {
                    is Avventende -> aktivitet.innspillTilArbeidsgiver
                    else -> null
                },
        )
    }

    internal fun konverterMedisinskVurdering(medisinskVurdering: MedisinskVurdering): MedisinskVurderingDTO =
        MedisinskVurderingDTO(
            hovedDiagnose = medisinskVurdering.hovedDiagnose?.let { konverterDiagnose(it) },
            biDiagnoser = medisinskVurdering.biDiagnoser?.map { konverterDiagnose(it) } ?: emptyList(),
            yrkesskade = medisinskVurdering.yrkesskade != null,
            yrkesskadeDato = medisinskVurdering.yrkesskade?.yrkesskadeDato,
            annenFraversArsak = medisinskVurdering.annenFraversArsak?.let { konverterAnnenFraversArsak(it) },
            svangerskap = medisinskVurdering.svangerskap,
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
                        AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND ->
                            AnnenFraverGrunnDTO.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND

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
            // TODO: sett når tsm har klart felt
            tekst = null,
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

    internal fun konverterKontaktMedPasient(tilbakedatering: Tilbakedatering): KontaktMedPasientDTO =
        KontaktMedPasientDTO(
            begrunnelseIkkeKontakt = tilbakedatering.begrunnelse,
            kontaktDato = tilbakedatering.kontaktDato,
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
        if (adresse == null) {
            return AdresseDTO(null, null, null, null, null)
        }
        return AdresseDTO(
            postnummer = adresse.postnummer?.toIntOrNull(),
            postboks = adresse.postboks,
            kommune = adresse.kommune,
            gate = adresse.gateadresse,
            land = adresse.land,
        )
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

fun ArbeidsgiverInfo.getMeldingTilArbeidsgiver(): String? =
    when (this) {
        is EnArbeidsgiver -> this.meldingTilArbeidsgiver
        is FlereArbeidsgivere -> this.meldingTilArbeidsgiver
        is IngenArbeidsgiver -> null
    }

fun ArbeidsgiverInfo.getTiltakArbeidsplassen(): String? =
    when (this) {
        is EnArbeidsgiver -> this.tiltakArbeidsplassen
        is FlereArbeidsgivere -> this.tiltakArbeidsplassen
        is IngenArbeidsgiver -> null
    }

fun ArbeidsgiverInfo.tilArbeidsgiverDTO(): ArbeidsgiverDTO? =
    when (this) {
        is FlereArbeidsgivere ->
            ArbeidsgiverDTO(
                navn = this.navn,
                stillingsprosent = this.stillingsprosent,
            )
        else -> null
    }
