package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.sykmelding.tsm.*
import no.nav.helse.flex.sykmelding.tsm.values.Adresse
import no.nav.helse.flex.sykmelding.tsm.values.Behandler
import no.nav.helse.flex.sykmelding.tsm.values.KontaktinfoType
import no.nav.helse.flex.sykmelding.tsm.values.Pasient
import no.nav.helse.flex.utils.logger
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
            is NorskSykmeldingGrunnlag -> konverterNorskSykmelding(sykmelding)
            is UtenlandskSykmeldingGrunnlag -> konverterUtenlandskSykmelding(sykmelding)
        }

    internal fun konverterTilSykmeldingDTO(
        sykmelding: Sykmelding,
        applySpesifikkMapping: SykmeldingDTO.() -> SykmeldingDTO = { this },
    ): SykmeldingDTO {
        val sykmeldingsperioder =
            sykmelding.sykmeldingGrunnlag.aktivitet.map { konverterSykmeldingsperiode(it) }
        val medisinskVurdering =
            konverterMedisinskVurdering(sykmelding.sykmeldingGrunnlag.medisinskVurdering)
        val metadata = sykmelding.sykmeldingGrunnlag.metadata

        return SykmeldingDTO(
            id = sykmelding.sykmeldingId,
            pasient =
                konverterPasient(
                    pasient = sykmelding.sykmeldingGrunnlag.pasient,
                    fom = sykmeldingsperioder.minBy { it.fom }.fom,
                ),
            mottattTidspunkt = metadata.mottattDato,
            behandlingsutfall = konverterBehandlingsutfall(sykmelding.validation),
            sykmeldingsperioder = sykmeldingsperioder,
            sykmeldingStatus =
                sykmeldingStatusDtoKonverterer.konverterSykmeldingStatus(sykmelding.sisteHendelse()),
            medisinskVurdering = medisinskVurdering,
            skjermesForPasient =
                sykmelding.sykmeldingGrunnlag.medisinskVurdering.skjermetForPasient,
            behandletTidspunkt = metadata.behandletTidspunkt ?: metadata.genDate,
            syketilfelleStartDato =
                sykmelding.sykmeldingGrunnlag.medisinskVurdering.syketilfelletStartDato,
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
            rulesetVersion = metadata.regelsettVersjon,
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
            utenlandskSykmelding = null,
        ).applySpesifikkMapping()
    }

    private fun konverterNorskSykmelding(sykmelding: Sykmelding): SykmeldingDTO {
        require(sykmelding.sykmeldingGrunnlag is NorskSykmeldingGrunnlag)

        val arbeidsgiver = sykmelding.sykmeldingGrunnlag.arbeidsgiver

        return konverterTilSykmeldingDTO(sykmelding) {
            copy(
                arbeidsgiver = arbeidsgiver.tilArbeidsgiverDTO(),
                prognose = sykmelding.sykmeldingGrunnlag.prognose?.let { konverterPrognose(it) },
                utdypendeOpplysninger =
                    konverterUtdypendeOpplysninger(
                        sykmelding.sykmeldingGrunnlag.utdypendeOpplysninger,
                    ),
                tiltakArbeidsplassen = arbeidsgiver.getTiltakArbeidsplassen(),
                tiltakNAV = sykmelding.sykmeldingGrunnlag.tiltak?.tiltakNav,
                andreTiltak = sykmelding.sykmeldingGrunnlag.tiltak?.andreTiltak,
                meldingTilNAV =
                    sykmelding.sykmeldingGrunnlag.bistandNav?.let { konverterMeldingTilNAV(it) },
                meldingTilArbeidsgiver = arbeidsgiver.getMeldingTilArbeidsgiver(),
                kontaktMedPasient =
                    sykmelding.sykmeldingGrunnlag.tilbakedatering?.let {
                        konverterKontaktMedPasient(it)
                    },
                behandler = konverterBehandler(sykmelding.sykmeldingGrunnlag.behandler),
            )
        }
    }

    private fun konverterUtenlandskSykmelding(sykmelding: Sykmelding): SykmeldingDTO {
        require(sykmelding.sykmeldingGrunnlag is UtenlandskSykmeldingGrunnlag)

        return konverterTilSykmeldingDTO(sykmelding) {
            copy(
                utenlandskSykmelding =
                    UtenlandskSykmelding(
                        land = sykmelding.sykmeldingGrunnlag.utenlandskInfo.land,
                    ),
            )
        }
    }

    internal fun konverterMerknader(validationResult: ValidationResult): List<MerknadDTO> =
        validationResult.rules
            .map {
                MerknadDTO(
                    type =
                        when (it.name) {
                            "TILBAKEDATERING_DELVIS_GODKJENT" -> {
                                MerknadtypeDTO.DELVIS_GODKJENT
                            }

                            "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER" -> {
                                MerknadtypeDTO.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER
                            }

                            "TILBAKEDATERING_UGYLDIG_TILBAKEDATERING" -> {
                                MerknadtypeDTO.UGYLDIG_TILBAKEDATERING
                            }

                            "TILBAKEDATERING_UNDER_BEHANDLING" -> {
                                MerknadtypeDTO.UNDER_BEHANDLING
                            }

                            "TILBAKEDATERING_TILBAKEDATERT_PAPIRSYKMELDING" -> {
                                MerknadtypeDTO.TILBAKEDATERT_PAPIRSYKMELDING
                            }

                            else -> {
                                MerknadtypeDTO.UKJENT_MERKNAD
                            }
                        },
                    beskrivelse =
                        when (it) {
                            is InvalidRule -> it.reason.sykmeldt
                            is PendingRule -> it.reason.sykmeldt
                            else -> null
                        },
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
                            if (rule.name !in RuleNameDTO.NAVN_SET) {
                                log.warn(
                                    "Ukjent regelnavn for 'InvalidRule': " + mapOf("regelNavn" to rule.name),
                                )
                            }

                            RegelinfoDTO(
                                messageForSender = rule.reason.sykmelder,
                                messageForUser = rule.reason.sykmeldt,
                                ruleName = rule.name,
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
                    if (aktivitet.medisinskArsak != null) {
                        MedisinskArsakDTO(
                            beskrivelse = aktivitet.medisinskArsak.beskrivelse,
                            arsak =
                                aktivitet.medisinskArsak.arsak.map {
                                    when (it) {
                                        MedisinskArsakType.TILSTAND_HINDRER_AKTIVITET -> {
                                            MedisinskArsakTypeDTO.TILSTAND_HINDRER_AKTIVITET
                                        }

                                        MedisinskArsakType.AKTIVITET_FORVERRER_TILSTAND -> {
                                            MedisinskArsakTypeDTO.AKTIVITET_FORVERRER_TILSTAND
                                        }

                                        MedisinskArsakType.AKTIVITET_FORHINDRER_BEDRING -> {
                                            MedisinskArsakTypeDTO.AKTIVITET_FORHINDRER_BEDRING
                                        }

                                        MedisinskArsakType.ANNET -> {
                                            MedisinskArsakTypeDTO.ANNET
                                        }
                                    }
                                },
                        )
                    } else {
                        null
                    }
                val arbeidsrelatertArsakDto =
                    if (aktivitet.arbeidsrelatertArsak != null) {
                        ArbeidsrelatertArsakDTO(
                            beskrivelse = aktivitet.arbeidsrelatertArsak.beskrivelse,
                            arsak =
                                aktivitet.arbeidsrelatertArsak.arsak.map {
                                    when (it) {
                                        ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING -> {
                                            ArbeidsrelatertArsakTypeDTO.MANGLENDE_TILRETTELEGGING
                                        }

                                        ArbeidsrelatertArsakType.ANNET -> {
                                            ArbeidsrelatertArsakTypeDTO.ANNET
                                        }
                                    }
                                },
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
                    is Gradert -> {
                        GradertDTO(
                            grad = aktivitet.grad,
                            reisetilskudd = aktivitet.reisetilskudd,
                        )
                    }

                    else -> {
                        null
                    }
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
            annenFraversArsak =
                when (medisinskVurdering) {
                    is DigitalMedisinskVurdering -> {
                        medisinskVurdering.annenFravarsgrunn?.let {
                            AnnenFraversArsakDTO(
                                null,
                                listOf(it.toAnnenFravarGrunnDTO()),
                            )
                        }
                    }

                    is IkkeDigitalMedisinskVurdering -> {
                        medisinskVurdering.annenFraversArsak?.let { konverterAnnenFraversArsak(it) }
                    }

                    else -> {
                        null
                    }
                },
            svangerskap = medisinskVurdering.svangerskap,
        )

    internal fun konverterAnnenFraversArsak(annenFraverArsak: AnnenFraverArsak): AnnenFraversArsakDTO? =
        AnnenFraversArsakDTO(
            beskrivelse = annenFraverArsak.beskrivelse,
            grunn =
                annenFraverArsak.arsak?.map { it.toAnnenFravarGrunnDTO() } ?: emptyList(),
        )

    private fun AnnenFravarArsakType.toAnnenFravarGrunnDTO(): AnnenFraverGrunnDTO =
        when (this) {
            AnnenFravarArsakType.GODKJENT_HELSEINSTITUSJON -> {
                AnnenFraverGrunnDTO.GODKJENT_HELSEINSTITUSJON
            }

            AnnenFravarArsakType.BEHANDLING_FORHINDRER_ARBEID -> {
                AnnenFraverGrunnDTO.BEHANDLING_FORHINDRER_ARBEID
            }

            AnnenFravarArsakType.ARBEIDSRETTET_TILTAK -> {
                AnnenFraverGrunnDTO.ARBEIDSRETTET_TILTAK
            }

            AnnenFravarArsakType.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND -> {
                AnnenFraverGrunnDTO.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND
            }

            AnnenFravarArsakType.NODVENDIG_KONTROLLUNDENRSOKELSE -> {
                AnnenFraverGrunnDTO.NODVENDIG_KONTROLLUNDENRSOKELSE
            }

            AnnenFravarArsakType.SMITTEFARE -> {
                AnnenFraverGrunnDTO.SMITTEFARE
            }

            AnnenFravarArsakType.ABORT -> {
                AnnenFraverGrunnDTO.ABORT
            }

            AnnenFravarArsakType.UFOR_GRUNNET_BARNLOSHET -> {
                AnnenFraverGrunnDTO.UFOR_GRUNNET_BARNLOSHET
            }

            AnnenFravarArsakType.DONOR -> {
                AnnenFraverGrunnDTO.DONOR
            }

            AnnenFravarArsakType.BEHANDLING_STERILISERING -> {
                AnnenFraverGrunnDTO.BEHANDLING_STERILISERING
            }
        }

    internal fun konverterDiagnose(diagnose: DiagnoseInfo): DiagnoseDTO =
        DiagnoseDTO(
            tekst = diagnose.tekst,
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
            SvarRestriksjon.SKJERMET_FOR_ARBEIDSGIVER -> {
                SvarRestriksjonDTO.SKJERMET_FOR_ARBEIDSGIVER
            }

            SvarRestriksjon.SKJERMET_FOR_PASIENT -> {
                SvarRestriksjonDTO.SKJERMET_FOR_PASIENT
            }

            SvarRestriksjon.SKJERMET_FOR_NAV -> {
                SvarRestriksjonDTO.SKJERMET_FOR_NAV
            }
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
        is EnArbeidsgiver -> {
            ArbeidsgiverDTO(
                navn = this.navn,
                stillingsprosent = this.stillingsprosent,
            )
        }

        is FlereArbeidsgivere -> {
            ArbeidsgiverDTO(
                navn = this.navn,
                stillingsprosent = this.stillingsprosent,
            )
        }

        is IngenArbeidsgiver -> {
            null
        }
    }
