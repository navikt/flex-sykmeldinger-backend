package no.nav.helse.flex.smregmapping

import no.nav.helse.flex.api.dto.MerknadtypeDTO
import no.nav.helse.flex.sykmelding.tsm.Aktivitet
import no.nav.helse.flex.sykmelding.tsm.AktivitetIkkeMulig
import no.nav.helse.flex.sykmelding.tsm.AktivitetType
import no.nav.helse.flex.sykmelding.tsm.ArbeidsgiverInfo
import no.nav.helse.flex.sykmelding.tsm.ArbeidsrelatertArsak
import no.nav.helse.flex.sykmelding.tsm.ArbeidsrelatertArsakType
import no.nav.helse.flex.sykmelding.tsm.AvsenderSystemNavn
import no.nav.helse.flex.sykmelding.tsm.Avventende
import no.nav.helse.flex.sykmelding.tsm.Behandlingsdager
import no.nav.helse.flex.sykmelding.tsm.EnArbeidsgiver
import no.nav.helse.flex.sykmelding.tsm.FlereArbeidsgivere
import no.nav.helse.flex.sykmelding.tsm.Gradert
import no.nav.helse.flex.sykmelding.tsm.IngenArbeidsgiver
import no.nav.helse.flex.sykmelding.tsm.InvalidRule
import no.nav.helse.flex.sykmelding.tsm.NorskSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.tsm.PendingRule
import no.nav.helse.flex.sykmelding.tsm.Prognose
import no.nav.helse.flex.sykmelding.tsm.Reisetilskudd
import no.nav.helse.flex.sykmelding.tsm.Rule
import no.nav.helse.flex.sykmelding.tsm.SykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.tsm.Tilbakedatering
import no.nav.helse.flex.sykmelding.tsm.UtenlandskSykmeldingGrunnlag
import no.nav.helse.flex.sykmelding.tsm.ValidationResult
import no.nav.helse.flex.sykmelding.tsm.values.Adresse
import no.nav.helse.flex.sykmelding.tsm.values.Behandler
import no.nav.helse.flex.sykmelding.tsm.values.KontaktinfoType
import no.nav.helse.flex.sykmelding.tsm.values.PersonIdType

object SykmeldingSmregKonverterer {
    fun konverterSykmelding(
        sykmeldingGrunnlag: SykmeldingGrunnlag,
        validation: ValidationResult? = null,
    ): SykmeldingSmregDto {
        val norskSykmelding = sykmeldingGrunnlag as? NorskSykmeldingGrunnlag
        val utenlandskSykmelding = sykmeldingGrunnlag as? UtenlandskSykmeldingGrunnlag

        return SykmeldingSmregDto(
            id = sykmeldingGrunnlag.id,
            mottattTidspunkt = sykmeldingGrunnlag.metadata.mottattDato,
            syketilfelleStartDato = sykmeldingGrunnlag.medisinskVurdering.syketilfelletStartDato,
            signaturDato = sykmeldingGrunnlag.metadata.genDate,
            behandletTidspunkt = sykmeldingGrunnlag.metadata.behandletTidspunkt ?: sykmeldingGrunnlag.metadata.genDate,
            sykmeldingsperioder = sykmeldingGrunnlag.aktivitet.map(::konverterSykmeldingsperiode),
            egenmeldt = sykmeldingGrunnlag.metadata.avsenderSystem.navn == AvsenderSystemNavn.EGENMELDT,
            papirsykmelding = sykmeldingGrunnlag.metadata.avsenderSystem.navn == AvsenderSystemNavn.PAPIRSYKMELDING,
            harRedusertArbeidsgiverperiode = false,
            merknader = validation?.rules?.let(::konverterMerknader),
            arbeidsgiver =
                norskSykmelding?.let { konverterArbeidsgiver(it.arbeidsgiver) }
                    ?: ArbeidsgiverSmregDto(null, null),
            prognose = norskSykmelding?.prognose?.let(::konverterPrognose),
            tiltakArbeidsplassen = norskSykmelding?.arbeidsgiver?.getTiltakArbeidsplassen(),
            meldingTilArbeidsgiver = norskSykmelding?.arbeidsgiver?.getMeldingTilArbeidsgiver(),
            kontaktMedPasient =
                norskSykmelding?.tilbakedatering?.let(::konverterKontaktMedPasient)
                    ?: KontaktMedPasientSmregDto(null),
            behandler = norskSykmelding?.behandler?.let(::konverterBehandler),
            utenlandskSykmelding = utenlandskSykmelding?.let(::konverterTilUtenlandskSykmelding),
        )
    }

    fun konverterTilUtenlandskSykmelding(sykmeldingGrunnlag: UtenlandskSykmeldingGrunnlag): UtenlandskSykmeldingSmregDto =
        UtenlandskSykmeldingSmregDto(
            land = sykmeldingGrunnlag.utenlandskInfo.land,
        )

    fun konverterSykmeldingsperiode(aktivitet: Aktivitet): SykmeldingsperiodeSmregDto {
        val aktivitetIkkeMuligDto =
            if (aktivitet is AktivitetIkkeMulig) {
                AktivitetIkkeMuligSmregDto(
                    arbeidsrelatertArsak = aktivitet.arbeidsrelatertArsak?.let(::konverterArbeidsrelatertArsak),
                )
            } else {
                null
            }
        return SykmeldingsperiodeSmregDto(
            fom = aktivitet.fom,
            tom = aktivitet.tom,
            type = konverterTilSykmeldingsperiodeType(aktivitet.type),
            aktivitetIkkeMulig = aktivitetIkkeMuligDto,
            gradert = konverterTilGradert(aktivitet),
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

    fun konverterTilSykmeldingsperiodeType(aktivitetType: AktivitetType): PeriodetypeSmregDto =
        when (aktivitetType) {
            AktivitetType.AKTIVITET_IKKE_MULIG -> PeriodetypeSmregDto.AKTIVITET_IKKE_MULIG
            AktivitetType.AVVENTENDE -> PeriodetypeSmregDto.AVVENTENDE
            AktivitetType.BEHANDLINGSDAGER -> PeriodetypeSmregDto.BEHANDLINGSDAGER
            AktivitetType.GRADERT -> PeriodetypeSmregDto.GRADERT
            AktivitetType.REISETILSKUDD -> PeriodetypeSmregDto.REISETILSKUDD
        }

    fun konverterTilGradert(aktivitet: Aktivitet): GradertSmregDto? =
        (aktivitet as? Gradert)?.let {
            GradertSmregDto(
                grad = aktivitet.grad,
                reisetilskudd = aktivitet.reisetilskudd,
            )
        }

    fun konverterArbeidsrelatertArsak(arbeidsrelatertArsak: ArbeidsrelatertArsak): ArbeidsrelatertArsakSmregDto =
        ArbeidsrelatertArsakSmregDto(
            beskrivelse = arbeidsrelatertArsak.beskrivelse,
            arsak = arbeidsrelatertArsak.arsak.map(::konverterArbeidsrelatertArsakType),
        )

    fun konverterArbeidsrelatertArsakType(arbeidsrelatertArsakType: ArbeidsrelatertArsakType): ArbeidsrelatertArsakTypeSmregDto =
        when (arbeidsrelatertArsakType) {
            ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING -> {
                ArbeidsrelatertArsakTypeSmregDto.MANGLENDE_TILRETTELEGGING
            }

            ArbeidsrelatertArsakType.ANNET -> {
                ArbeidsrelatertArsakTypeSmregDto.ANNET
            }
        }

    fun konverterPrognose(prognose: Prognose): PrognoseSmregDto =
        PrognoseSmregDto(
            arbeidsforEtterPeriode = prognose.arbeidsforEtterPeriode,
            hensynArbeidsplassen = prognose.hensynArbeidsplassen,
        )

    fun konverterArbeidsgiver(arbeidsgiver: ArbeidsgiverInfo): ArbeidsgiverSmregDto {
        val (navn, yrkesbetegnelse) =
            when (arbeidsgiver) {
                is EnArbeidsgiver -> Pair(arbeidsgiver.navn, arbeidsgiver.yrkesbetegnelse)
                is FlereArbeidsgivere -> Pair(arbeidsgiver.navn, arbeidsgiver.yrkesbetegnelse)
                is IngenArbeidsgiver -> Pair(null, null)
            }
        return ArbeidsgiverSmregDto(navn, yrkesbetegnelse)
    }

    fun konverterBehandler(behandler: Behandler): BehandlerSmregDto {
        val hpr = behandler.ids.find { it.type == PersonIdType.HPR }?.id
        return BehandlerSmregDto(
            fornavn = behandler.navn.fornavn,
            mellomnavn = behandler.navn.mellomnavn,
            etternavn = behandler.navn.etternavn,
            hpr = hpr,
            adresse = konverterAdresse(behandler.adresse),
            tlf = behandler.kontaktinfo.find { it.type == KontaktinfoType.TLF }?.value,
        )
    }

    fun konverterKontaktMedPasient(tilbakedatering: Tilbakedatering): KontaktMedPasientSmregDto =
        KontaktMedPasientSmregDto(
            kontaktDato = tilbakedatering.kontaktDato,
        )

    fun konverterAdresse(adresse: Adresse?): AdresseSmregDto {
        if (adresse == null) {
            return AdresseSmregDto(null, null, null, null, null)
        }
        return AdresseSmregDto(
            gate = adresse.gateadresse,
            postnummer = adresse.postnummer?.toIntOrNull(),
            kommune = adresse.kommune,
            postboks = adresse.postboks,
            land = adresse.land,
        )
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

    fun konverterMerknader(validationRules: List<Rule>): List<MerknadSmregDto> =
        validationRules
            .map { rule ->
                MerknadSmregDto(
                    type = konverterMerknadType(rule.name),
                    beskrivelse =
                        when (rule) {
                            is InvalidRule -> rule.reason.sykmeldt
                            is PendingRule -> rule.reason.sykmeldt
                            else -> null
                        },
                )
            }

    fun konverterMerknadType(validationRuleName: String): String =
        when (validationRuleName) {
            "TILBAKEDATERING_DELVIS_GODKJENT" -> {
                MerknadtypeSmregDto.DELVIS_GODKJENT.name
            }

            "TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER" -> {
                MerknadtypeSmregDto.TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER.name
            }

            "TILBAKEDATERING_UGYLDIG_TILBAKEDATERING" -> {
                MerknadtypeSmregDto.UGYLDIG_TILBAKEDATERING.name
            }

            "TILBAKEDATERING_UNDER_BEHANDLING" -> {
                MerknadtypeSmregDto.UNDER_BEHANDLING.name
            }

            "TILBAKEDATERING_TILBAKEDATERT_PAPIRSYKMELDING" -> {
                MerknadtypeSmregDto.TILBAKEDATERT_PAPIRSYKMELDING.name
            }

            else -> {
                MerknadtypeDTO.UKJENT_MERKNAD.name
            }
        }
}
