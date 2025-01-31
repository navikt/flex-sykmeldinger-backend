package no.nav.helse.flex.sykmelding.api

import no.nav.helse.flex.sykmelding.api.dto.*
import no.nav.helse.flex.sykmelding.api.dto.ArbeidsgiverStatusDTO
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.domain.SporsmalSvar
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class SykmeldingDtoKonverterer {
    fun konverter(sykmelding: Sykmelding): SykmeldingDTO =
        when (sykmelding.sykmeldingGrunnlag) {
            is SykmeldingGrunnlag -> konverterSykmelding(sykmelding)
            is UtenlandskSykmeldingGrunnlag -> konverterUtenlandskSykmelding(sykmelding)
        }

    internal fun konverterSykmelding(sykmelding: Sykmelding): SykmeldingDTO {
        require(sykmelding.sykmeldingGrunnlag is SykmeldingGrunnlag)
        return SykmeldingDTO(
            id = sykmelding.sykmeldingId,
            pasient = konverterPasient(sykmelding.sykmeldingGrunnlag.pasient),
            mottattTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.mottattDato,
            behandlingsutfall = konverterBehandlingsutfall(sykmelding),
            // TODO
            legekontorOrgnummer = null,
            arbeidsgiver =
                konverterArbeidsgiver(
                    sykmelding.sykmeldingGrunnlag.arbeidsgiver,
                ),
            sykmeldingsperioder = sykmelding.sykmeldingGrunnlag.aktivitet.map { konverterSykmeldingsperiode(it) },
            sykmeldingStatus = konverterSykmeldingStatus(sykmelding.sisteStatus()),
            medisinskVurdering = konverterMedisinskVurdering(sykmelding.sykmeldingGrunnlag.medisinskVurdering),
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
            kontaktMedPasient = konverterKontaktMedPasient(),
            behandletTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.behandletTidspunkt,
            behandler = konverterBehandler(sykmelding.sykmeldingGrunnlag.behandler),
            syketilfelleStartDato =
                sykmelding.sykmeldingGrunnlag.metadata.genDate
                    .toLocalDate(),
            navnFastlege = sykmelding.sykmeldingGrunnlag.pasient.navnFastlege,
            egenmeldt = null,
            papirsykmelding = false,
            harRedusertArbeidsgiverperiode = null,
            merknader = null,
            rulesetVersion = sykmelding.sykmeldingGrunnlag.metadata.regelsettVersjon,
            utenlandskSykmelding = null,
        )
    }

    internal fun konverterUtenlandskSykmelding(sykmelding: Sykmelding): SykmeldingDTO {
        require(sykmelding.sykmeldingGrunnlag is UtenlandskSykmeldingGrunnlag)
        return SykmeldingDTO(
            id = sykmelding.sykmeldingId,
            pasient = konverterPasient(sykmelding.sykmeldingGrunnlag.pasient),
            mottattTidspunkt = sykmelding.sykmeldingGrunnlag.metadata.mottattDato,
            behandlingsutfall = konverterBehandlingsutfall(sykmelding),
            legekontorOrgnummer = null,
            arbeidsgiver = null,
            sykmeldingsperioder = sykmelding.sykmeldingGrunnlag.aktivitet.map { konverterSykmeldingsperiode(it) },
            sykmeldingStatus = konverterSykmeldingStatus(sykmelding.sisteStatus()),
            medisinskVurdering = konverterMedisinskVurdering(sykmelding.sykmeldingGrunnlag.medisinskVurdering),
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
            egenmeldt = null,
            papirsykmelding = false,
            harRedusertArbeidsgiverperiode = null,
            merknader = null,
            rulesetVersion = sykmelding.sykmeldingGrunnlag.metadata.regelsettVersjon,
            utenlandskSykmelding =
                UtenlandskSykmelding(
                    land = sykmelding.sykmeldingGrunnlag.utenlandskInfo.land,
                ),
        )
    }

    fun konverterSykmeldingStatus(status: SykmeldingHendelse): SykmeldingStatusDTO =
        SykmeldingStatusDTO(
            // TODO
            statusEvent = status.status.name,
            timestamp = status.opprettet.atOffset(ZoneOffset.UTC),
            sporsmalOgSvarListe = emptyList(),
            arbeidsgiver =
                status.arbeidstakerInfo?.arbeidsgiver?.let { arbeidsgiver ->
                    ArbeidsgiverStatusDTO(
                        orgnummer = arbeidsgiver.orgnummer,
                        juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
                        orgNavn = arbeidsgiver.orgnavn,
                    )
                },
            brukerSvar = null,
        )

    internal fun konverterSykmeldingSporsmal(sporsmal: List<Sporsmal>): SykmeldingSporsmalSvarDto {
        fun hentSporsmal(
            sporsmal: List<Sporsmal>,
            tag: SporsmalTag,
        ): Sporsmal? = sporsmal.find { it.tag == tag }

        fun <T> Sporsmal.tilSvar(mapper: (verdi: String) -> T): FormSporsmalSvar<T>? {
            val forsteSvarVerdi: String? = this.forsteSvarVerdi
            return if (forsteSvarVerdi == null) {
                null
            } else {
                FormSporsmalSvar(sporsmaltekst = this.sporsmalstekst ?: "", svar = mapper(forsteSvarVerdi))
            }
        }

        fun Sporsmal.tilJaNeiSvar(): FormSporsmalSvar<JaEllerNei>? = this.tilSvar { enumValueOf(it) }

        SykmeldingSporsmalSvarDto(
            erOpplysningeneRiktige =
                hentSporsmal(sporsmal, SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE)?.tilJaNeiSvar()
                    ?: error("ER_OPPLYSNINGENE_RIKTIGE må ha svar"),
            uriktigeOpplysninger =
                hentSporsmal(sporsmal, SporsmalTag.URIKTIGE_OPPLYSNINGER)?.let {
                    val verdier =
                        it.svar.map { svar ->
                            enumValueOf<UriktigeOpplysningerType>(svar.verdi)
                        }
                    FormSporsmalSvar(
                        sporsmaltekst = it.sporsmalstekst ?: "",
                        svar = verdier,
                    )
                },
            arbeidssituasjon = TODO(),
            arbeidsgiverOrgnummer = TODO(),
            arbeidsledig = TODO(),
            riktigNarmesteLeder = hentSporsmal(sporsmal, SporsmalTag.RIKTIG_NARMESTE_LEDER)?.tilJaNeiSvar(),
            harBruktEgenmelding = TODO(),
            egenmeldingsperioder = TODO(),
            harForsikring = TODO(),
            egenmeldingsdager = TODO(),
            harBruktEgenmeldingsdager = TODO(),
            fisker = TODO(),
        )
    }

    internal fun konverterPasient(pasient: Pasient): PasientDTO =
        PasientDTO(
            fnr = pasient.fnr,
            fornavn = pasient.navn?.fornavn,
            mellomnavn = pasient.navn?.mellomnavn,
            etternavn = pasient.navn?.etternavn,
            // TODO
            overSyttiAar = null,
        )

    internal fun konverterTiltakArbeidsplassen(arbeidsgiver: ArbeidsgiverInfo): String? =
        when (arbeidsgiver) {
            is EnArbeidsgiver -> arbeidsgiver.tiltakArbeidsplassen
            is FlereArbeidsgivere -> arbeidsgiver.tiltakArbeidsplassen
            is IngenArbeidsgiver -> null
        }

    internal fun konverterBehandlingsutfall(sykmelding: Sykmelding): BehandlingsutfallDTO =
        BehandlingsutfallDTO(
            // TODO: benytt behandlingsutfall fra tsm kafka melding
            status = RegelStatusDTO.OK,
            ruleHits = emptyList(),
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
        if (adresse == null) return AdresseDTO(null, null, null, null, null)
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
}
