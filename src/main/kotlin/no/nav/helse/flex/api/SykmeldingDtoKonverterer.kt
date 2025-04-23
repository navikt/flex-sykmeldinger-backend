package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.tsm.*
import no.nav.helse.flex.sykmelding.domain.tsm.SporsmalSvar
import no.nav.helse.flex.sykmelding.domain.tsm.values.Adresse
import no.nav.helse.flex.sykmelding.domain.tsm.values.Behandler
import no.nav.helse.flex.sykmelding.domain.tsm.values.KontaktinfoType
import no.nav.helse.flex.sykmelding.domain.tsm.values.Pasient
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Month

@Component
class SykmeldingDtoKonverterer(
    private val sykmeldingStatusDtoKonverterer: SykmeldingStatusDtoKonverterer,
) {
    fun konverter(sykmelding: Sykmelding): SykmeldingDTO =
        when (sykmelding.sykmeldingGrunnlag) {
            is SykmeldingGrunnlag -> konverterSykmelding(sykmelding)
            is UtenlandskSykmeldingGrunnlag -> konverterUtenlandskSykmelding(sykmelding)
        }

    internal fun konverterSykmelding(sykmelding: Sykmelding): SykmeldingDTO {
        require(sykmelding.sykmeldingGrunnlag is SykmeldingGrunnlag)
        val sykmeldingsperioder = sykmelding.sykmeldingGrunnlag.aktivitet.map { konverterSykmeldingsperiode(it) }
        val medisinskVurdering = konverterMedisinskVurdering(sykmelding.sykmeldingGrunnlag.medisinskVurdering)

        return SykmeldingDTO(
            id = sykmelding.sykmeldingId,
            // TODO: Hvordan beregnes overSøttiÅr?
            pasient =
                konverterPasient(
                    pasient = sykmelding.sykmeldingGrunnlag.pasient,
                    fom = sykmeldingsperioder.minBy { it.fom }.fom,
                ),
            mottattTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.mottattDato,
            behandlingsutfall = konverterBehandlingsutfall(sykmelding.validation),
            // TODO: muligens reciever eller sender fra meldingsinformasjon. tror vi skal ignorere
            legekontorOrgnummer = null,
            // TODO: Er dette arbeidsgiver fra sykmeldingGrunnlag eller basert på brukers svar?
            arbeidsgiver =
                konverterArbeidsgiver(
                    sykmelding.sykmeldingGrunnlag.arbeidsgiver,
                ),
            sykmeldingsperioder = sykmeldingsperioder,
            sykmeldingStatus = sykmeldingStatusDtoKonverterer.konverterSykmeldingStatus(sykmelding.sisteHendelse()),
            medisinskVurdering = medisinskVurdering,
            skjermesForPasient = sykmelding.sykmeldingGrunnlag.medisinskVurdering.skjermetForPasient,
            prognose = sykmelding.sykmeldingGrunnlag.prognose?.let { konverterPrognose(it) },
            utdypendeOpplysninger = konverterUtdypendeOpplysninger(sykmelding.sykmeldingGrunnlag.utdypendeOpplysninger),
            tiltakArbeidsplassen = konverterTiltakArbeidsplassen(sykmelding.sykmeldingGrunnlag.arbeidsgiver),
            tiltakNAV = sykmelding.sykmeldingGrunnlag.tiltak?.tiltakNAV,
            andreTiltak = sykmelding.sykmeldingGrunnlag.tiltak?.andreTiltak,
            meldingTilNAV = sykmelding.sykmeldingGrunnlag.bistandNav?.let { konverterMeldingTilNAV(it) },
            meldingTilArbeidsgiver =
                when (sykmelding.sykmeldingGrunnlag.arbeidsgiver) {
                    is EnArbeidsgiver -> sykmelding.sykmeldingGrunnlag.arbeidsgiver.meldingTilArbeidsgiver
                    is FlereArbeidsgivere -> sykmelding.sykmeldingGrunnlag.arbeidsgiver.meldingTilArbeidsgiver
                    is IngenArbeidsgiver -> null
                },
            // TODO: Hvor er kontakt med pasient?
            kontaktMedPasient = konverterKontaktMedPasient(),
            behandletTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.behandletTidspunkt,
            behandler = konverterBehandler(sykmelding.sykmeldingGrunnlag.behandler),
            // TODO: Er dette riktig?
            syketilfelleStartDato =
                sykmelding.sykmeldingGrunnlag.metadata.genDate
                    .toLocalDate(),
            navnFastlege = sykmelding.sykmeldingGrunnlag.pasient.navnFastlege,
            egenmeldt = sykmelding.avsenderSystemNavn == AvsenderSystemNavn.EGENMELDT,
            papirsykmelding = sykmelding.avsenderSystemNavn == AvsenderSystemNavn.PAPIRSYKMELDING,
            // TODO: gjelder dette bare for covid?
            harRedusertArbeidsgiverperiode =
                harRedusertArbeidsgiverperiode(
                    hovedDiagnose = medisinskVurdering.hovedDiagnose,
                    biDiagnoser = medisinskVurdering.biDiagnoser,
                    sykmeldingsperioder = sykmeldingsperioder,
                    annenFraversArsakDTO = medisinskVurdering.annenFraversArsak,
                ),
            // TODO: Brukes disse?
            merknader = null,
            rulesetVersion = sykmelding.sykmeldingGrunnlag.metadata.regelsettVersjon,
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
            legekontorOrgnummer = null,
            arbeidsgiver = null,
            sykmeldingsperioder = sykmeldingsperioder,
            sykmeldingStatus = sykmeldingStatusDtoKonverterer.konverterSykmeldingStatus(sykmelding.sisteHendelse()),
            medisinskVurdering = medisinskVurdering,
            skjermesForPasient = sykmelding.sykmeldingGrunnlag.medisinskVurdering.skjermetForPasient,
            prognose = null,
            utdypendeOpplysninger = emptyMap(),
            tiltakArbeidsplassen = null,
            tiltakNAV = null,
            andreTiltak = null,
            meldingTilNAV = null,
            meldingTilArbeidsgiver = null,
            kontaktMedPasient =
                KontaktMedPasientDTO(
                    kontaktDato = null,
                    begrunnelseIkkeKontakt = null,
                ),
            behandletTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.behandletTidspunkt,
            // TODO
            behandler =
                BehandlerDTO(
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "Etternavn",
                    adresse =
                        AdresseDTO(
                            gate = null,
                            postnummer = null,
                            kommune = null,
                            postboks = null,
                            land = null,
                        ),
                    tlf = null,
                ),
            syketilfelleStartDato =
                sykmelding.sykmeldingGrunnlag.metadata.genDate
                    .toLocalDate(),
            navnFastlege = sykmelding.sykmeldingGrunnlag.pasient.navnFastlege,
            egenmeldt = sykmelding.avsenderSystemNavn == AvsenderSystemNavn.EGENMELDT,
            papirsykmelding = sykmelding.avsenderSystemNavn == AvsenderSystemNavn.PAPIRSYKMELDING,
            harRedusertArbeidsgiverperiode =
                harRedusertArbeidsgiverperiode(
                    hovedDiagnose = medisinskVurdering.hovedDiagnose,
                    biDiagnoser = medisinskVurdering.biDiagnoser,
                    sykmeldingsperioder = sykmeldingsperioder,
                    annenFraversArsakDTO = medisinskVurdering.annenFraversArsak,
                ),
            merknader = null,
            rulesetVersion = sykmelding.sykmeldingGrunnlag.metadata.regelsettVersjon,
            utenlandskSykmelding =
                UtenlandskSykmelding(
                    land = sykmelding.sykmeldingGrunnlag.utenlandskInfo.land,
                ),
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
            overSyttiAar = erOverSyttiAar(fom),
        )

    internal fun erOverSyttiAar(fom: LocalDate): Boolean {
        // TODO: kall pdl
        return false
    }

    internal fun konverterTiltakArbeidsplassen(arbeidsgiver: ArbeidsgiverInfo): String? =
        when (arbeidsgiver) {
            is EnArbeidsgiver -> arbeidsgiver.tiltakArbeidsplassen
            is FlereArbeidsgivere -> arbeidsgiver.tiltakArbeidsplassen
            is IngenArbeidsgiver -> null
        }

    internal fun konverterBehandlingsutfall(validationResult: ValidationResult): BehandlingsutfallDTO {
        val status =
            when (validationResult.status) {
                RuleType.OK -> RegelStatusDTO.OK
                RuleType.PENDING -> RegelStatusDTO.MANUAL_PROCESSING
                RuleType.INVALID -> RegelStatusDTO.INVALID
            }
        return BehandlingsutfallDTO(
            status = status,
            ruleHits =
                validationResult.rules.map {
                    // TODO: blir det riktig å bruke description her?
                    RegelinfoDTO(
                        messageForSender = it.description,
                        messageForUser = it.description,
                        ruleName = it.name,
                        ruleStatus = status,
                    )
                },
        )
    }

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
                                        ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING ->
                                            ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING

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

    internal fun konverterKontaktMedPasient(): KontaktMedPasientDTO =
        KontaktMedPasientDTO(
            // TODO
            begrunnelseIkkeKontakt = null,
            kontaktDato = null,
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

    internal fun konverterArbeidsgiver(arbeidsgiverInfo: ArbeidsgiverInfo): ArbeidsgiverDTO? =
        when (arbeidsgiverInfo) {
            is FlereArbeidsgivere ->
                ArbeidsgiverDTO(
                    // TODO: Hva blir riktig for flere arbeidsgivere?
                    navn = arbeidsgiverInfo.navn,
                    stillingsprosent = arbeidsgiverInfo.stillingsprosent,
                )

            is EnArbeidsgiver ->
                ArbeidsgiverDTO(
                    navn = null,
                    stillingsprosent = null,
                )

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

    internal fun harRedusertArbeidsgiverperiode(
        hovedDiagnose: DiagnoseDTO?,
        biDiagnoser: List<DiagnoseDTO>,
        sykmeldingsperioder: List<SykmeldingsperiodeDTO>,
        annenFraversArsakDTO: AnnenFraversArsakDTO?,
    ): Boolean {
        val diagnoserSomGirRedusertArbgiverPeriode = listOf("R991", "U071", "U072", "A23", "R992")

        val sykmeldingsperioderInnenforKoronaregler =
            sykmeldingsperioder.filter { periodeErInnenforKoronaregler(it.fom, it.tom) }
        if (sykmeldingsperioderInnenforKoronaregler.isEmpty()) {
            return false
        }
        if (
            hovedDiagnose != null &&
            diagnoserSomGirRedusertArbgiverPeriode.contains(hovedDiagnose.kode)
        ) {
            return true
        } else if (
            biDiagnoser.isNotEmpty() &&
            biDiagnoser.find { diagnoserSomGirRedusertArbgiverPeriode.contains(it.kode) } !=
            null
        ) {
            return true
        }
        return checkSmittefare(annenFraversArsakDTO)
    }

    private fun checkSmittefare(annenFraversArsakDTO: AnnenFraversArsakDTO?) =
        annenFraversArsakDTO?.grunn?.any { annenFraverGrunn ->
            annenFraverGrunn == AnnenFraverGrunnDTO.SMITTEFARE
        } == true

    private fun periodeErInnenforKoronaregler(
        fom: LocalDate,
        tom: LocalDate,
    ): Boolean {
        val koronaForsteFraDato = LocalDate.of(2020, Month.MARCH, 15)
        val koronaForsteTilDato = LocalDate.of(2021, Month.OCTOBER, 1)
        val koronaAndreFraDato = LocalDate.of(2021, Month.NOVEMBER, 30)
        val koronaAndreTilDato = LocalDate.of(2022, Month.JULY, 1)

        return (fom.isAfter(koronaAndreFraDato) && fom.isBefore(koronaAndreTilDato)) ||
            (fom.isBefore(koronaForsteTilDato) && tom.isAfter(koronaForsteFraDato))
    }
}
