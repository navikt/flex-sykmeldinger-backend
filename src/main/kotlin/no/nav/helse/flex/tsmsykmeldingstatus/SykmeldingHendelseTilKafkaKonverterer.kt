package no.nav.helse.flex.tsmsykmeldingstatus

import no.nav.helse.flex.api.SykmeldingStatusDtoKonverterer
import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.config.tilNorgeOffsetDateTime
import no.nav.helse.flex.sykmelding.application.BrukerSvar
import no.nav.helse.flex.sykmelding.application.FiskerBrukerSvar
import no.nav.helse.flex.sykmelding.application.FiskerLottOgHyre
import no.nav.helse.flex.sykmelding.application.UtdatertFormatBrukerSvar
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer.tilBakoverkompatibelSendtStatus
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer.tilStatusEventDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.*
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.utils.serialisertTilString

object SykmeldingHendelseTilKafkaKonverterer {
    private val logger = this.logger()

    fun konverterSykmeldingHendelseTilKafkaDTO(
        sykmeldingHendelse: SykmeldingHendelse,
        sykmeldingId: String,
    ): SykmeldingStatusKafkaDTO =
        when (sykmeldingHendelse.status) {
            HendelseStatus.SENDT_TIL_NAV,
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER,
            -> {
                konverterSykmeldinghendelseStatusSendt(
                    hendelse = sykmeldingHendelse,
                    sykmeldingId = sykmeldingId,
                )
            }
            HendelseStatus.BEKREFTET_AVVIST -> {
                SykmeldingStatusKafkaDTO(
                    sykmeldingId = sykmeldingId,
                    timestamp = sykmeldingHendelse.hendelseOpprettet.tilNorgeOffsetDateTime(),
                    statusEvent = sykmeldingHendelse.status.tilStatusEventDTO(),
                    arbeidsgiver = null,
                    sporsmals = emptyList(),
                )
            }
            HendelseStatus.APEN,
            HendelseStatus.AVBRUTT,
            -> {
                SykmeldingStatusKafkaDTO(
                    sykmeldingId = sykmeldingId,
                    timestamp = sykmeldingHendelse.hendelseOpprettet.tilNorgeOffsetDateTime(),
                    statusEvent = sykmeldingHendelse.status.tilStatusEventDTO(),
                    arbeidsgiver = null,
                    sporsmals = null,
                )
            }
            HendelseStatus.UTGATT -> throw NotImplementedError(
                "HendelseStatus.UTGATT er ikke implementert. Vi har ikke funnet kilden til hvordan denne håndeteres. For sykmelding: $sykmeldingId",
            )
        }

    internal fun konverterSykmeldinghendelseStatusSendt(
        hendelse: SykmeldingHendelse,
        sykmeldingId: String,
    ): SykmeldingStatusKafkaDTO {
        requireNotNull(hendelse.brukerSvar) {
            "BrukerSvar kan ikke være null for hendelsestatus ${hendelse.status}. For sykmelding: $sykmeldingId"
        }
        require(hendelse.brukerSvar !is UtdatertFormatBrukerSvar) {
            "UtdatertFormatBrukerSvar er ikke støttet. For sykmelding: $sykmeldingId"
        }
        require(hendelse.tilleggsinfo !is UtdatertFormatTilleggsinfo) {
            "UtdatertFormatTilleggsinfo er ikke støttet. For sykmelding: $sykmeldingId"
        }

        val arbeidsgiver: Arbeidsgiver? =
            when (hendelse.tilleggsinfo) {
                is ArbeidstakerTilleggsinfo -> hendelse.tilleggsinfo.arbeidsgiver
                is FiskerTilleggsinfo -> hendelse.tilleggsinfo.arbeidsgiver
                else -> null
            }
        val tidligereArbeidsgiver: TidligereArbeidsgiver? =
            when (hendelse.tilleggsinfo) {
                is ArbeidsledigTilleggsinfo -> hendelse.tilleggsinfo.tidligereArbeidsgiver
                is PermittertTilleggsinfo -> hendelse.tilleggsinfo.tidligereArbeidsgiver
                else -> null
            }
        val sporsmalSvarDto = SykmeldingStatusDtoKonverterer().konverterSykmeldingSporsmalSvar(hendelse.brukerSvar)
        return SykmeldingStatusKafkaDTO(
            sykmeldingId = sykmeldingId,
            timestamp = hendelse.hendelseOpprettet.tilNorgeOffsetDateTime(),
            statusEvent =
                hendelse.status
                    .tilBakoverkompatibelSendtStatus(hendelse.brukerSvar)
                    .tilStatusEventDTO(),
            sporsmals =
                konverterTilSporsmalsKafkaDto(
                    sporsmalSvarDto = sporsmalSvarDto,
                    sykmeldingId = sykmeldingId,
                    harAktivtArbeidsforhold = arbeidsgiver?.erAktivtArbeidsforhold,
                ),
            brukerSvar = konverterTilBrukerSvarKafkaDTO(sporsmalSvarDto),
            arbeidsgiver = arbeidsgiver?.tilArbeidsgiverKafkaDto(),
            tidligereArbeidsgiver = tidligereArbeidsgiver?.tilTidligereArbeidsgiverKafkaDto(sykmeldingId),
        )
    }

    internal fun konverterTilBrukerSvarKafkaDTO(sykmeldingSporsmalSvarDto: SykmeldingSporsmalSvarDto): BrukerSvarKafkaDTO =
        BrukerSvarKafkaDTO(
            erOpplysningeneRiktige = sykmeldingSporsmalSvarDto.erOpplysningeneRiktige,
            uriktigeOpplysninger = sykmeldingSporsmalSvarDto.uriktigeOpplysninger,
            arbeidssituasjon = sykmeldingSporsmalSvarDto.arbeidssituasjon,
            arbeidsgiverOrgnummer = sykmeldingSporsmalSvarDto.arbeidsgiverOrgnummer,
            riktigNarmesteLeder = sykmeldingSporsmalSvarDto.riktigNarmesteLeder,
            harBruktEgenmelding = sykmeldingSporsmalSvarDto.harBruktEgenmelding,
            egenmeldingsperioder = sykmeldingSporsmalSvarDto.egenmeldingsperioder,
            harForsikring = sykmeldingSporsmalSvarDto.harForsikring,
            egenmeldingsdager = sykmeldingSporsmalSvarDto.egenmeldingsdager,
            harBruktEgenmeldingsdager = sykmeldingSporsmalSvarDto.harBruktEgenmeldingsdager,
            fisker =
                sykmeldingSporsmalSvarDto.fisker?.let {
                    FiskereSvarKafkaDTO(
                        blad = it.blad,
                        lottOgHyre = it.lottOgHyre,
                    )
                },
        )

    internal fun konverterTilSporsmalsKafkaDto(
        sporsmalSvarDto: SykmeldingSporsmalSvarDto,
        harAktivtArbeidsforhold: Boolean? = null,
        sykmeldingId: String? = null,
    ): List<SporsmalKafkaDTO> =
        listOfNotNull(
            sporsmalSvarDto.arbeidssituasjonSporsmalBuilder(),
            sporsmalSvarDto.harBruktEgenmeldingSporsmalBuilder(),
            sporsmalSvarDto.egenmeldingsPeriodeSporsmalBuilder(),
            sporsmalSvarDto.riktigNarmesteLederSporsmalBuilder(
                sykmeldingId = sykmeldingId,
                erAktivtArbidsforhold = harAktivtArbeidsforhold,
            ),
            sporsmalSvarDto.forsikringSporsmalBuilder(),
            sporsmalSvarDto.egenmeldingsdagerBuilder(),
        )

    private fun HendelseStatus.tilBakoverkompatibelSendtStatus(brukerSvar: BrukerSvar?): HendelseStatus {
        val bakoverkompatibelStatus =
            when (brukerSvar) {
                is FiskerBrukerSvar -> {
                    if (brukerSvar.lottOgHyre.svar == FiskerLottOgHyre.HYRE) {
                        HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    } else {
                        HendelseStatus.SENDT_TIL_NAV
                    }
                }
                else -> this
            }
        return bakoverkompatibelStatus
    }

    private fun Arbeidsgiver.tilArbeidsgiverKafkaDto(): ArbeidsgiverStatusKafkaDTO =
        ArbeidsgiverStatusKafkaDTO(
            orgnummer = orgnummer,
            juridiskOrgnummer = juridiskOrgnummer,
            orgNavn = orgnavn,
        )

    private fun TidligereArbeidsgiver.tilTidligereArbeidsgiverKafkaDto(sykmeldingId: String): TidligereArbeidsgiverKafkaDTO =
        TidligereArbeidsgiverKafkaDTO(
            orgNavn = orgNavn,
            orgnummer = orgnummer,
            sykmeldingsId = sykmeldingId,
        )

    private fun HendelseStatus.tilStatusEventDTO(): String =
        when (this) {
            HendelseStatus.SENDT_TIL_NAV -> "BEKREFTET"
            HendelseStatus.BEKREFTET_AVVIST -> "BEKREFTET"
            HendelseStatus.APEN -> "APEN"
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER -> "SENDT"
            HendelseStatus.AVBRUTT -> "AVBRUTT"
            HendelseStatus.UTGATT -> "UTGATT"
        }

    private fun SykmeldingSporsmalSvarDto.egenmeldingsdagerBuilder(): SporsmalKafkaDTO? {
        if (egenmeldingsdager == null) return null

        return SporsmalKafkaDTO(
            tekst = egenmeldingsdager.sporsmaltekst,
            shortName = ShortNameKafkaDTO.EGENMELDINGSDAGER,
            svartype = SvartypeKafkaDTO.DAGER,
            svar = egenmeldingsdager.svar.serialisertTilString(),
        )
    }

    private fun SykmeldingSporsmalSvarDto.arbeidssituasjonSporsmalBuilder(): SporsmalKafkaDTO {
        // In the old sporsmal and svar list, fiskere should be mapped to ARBEIDSTAKER or
        // NAERINGSDRIVENDE, dependening on whether or not they are working on lott or hyre.
        val normalisertSituasjon: ArbeidssituasjonDTO =
            when (arbeidssituasjon.svar) {
                ArbeidssituasjonDTO.FISKER -> {
                    when (fisker?.lottOgHyre?.svar) {
                        LottOgHyre.HYRE -> ArbeidssituasjonDTO.ARBEIDSTAKER
                        else -> ArbeidssituasjonDTO.NAERINGSDRIVENDE
                    }
                }
                ArbeidssituasjonDTO.JORDBRUKER -> ArbeidssituasjonDTO.NAERINGSDRIVENDE
                else -> arbeidssituasjon.svar
            }

        return SporsmalKafkaDTO(
            tekst = arbeidssituasjon.sporsmaltekst,
            shortName = ShortNameKafkaDTO.ARBEIDSSITUASJON,
            svartype = SvartypeKafkaDTO.ARBEIDSSITUASJON,
            svar = normalisertSituasjon.name,
        )
    }

    private fun SykmeldingSporsmalSvarDto.harBruktEgenmeldingSporsmalBuilder(): SporsmalKafkaDTO? {
        if (harBruktEgenmelding != null) {
            return SporsmalKafkaDTO(
                tekst = harBruktEgenmelding.sporsmaltekst,
                shortName = ShortNameKafkaDTO.FRAVAER,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar = harBruktEgenmelding.svar.name,
            )
        }
        return null
    }

    private fun SykmeldingSporsmalSvarDto.egenmeldingsPeriodeSporsmalBuilder(): SporsmalKafkaDTO? {
        if (egenmeldingsperioder != null) {
            return SporsmalKafkaDTO(
                tekst = egenmeldingsperioder.sporsmaltekst,
                shortName = ShortNameKafkaDTO.PERIODE,
                svartype = SvartypeKafkaDTO.PERIODER,
                svar = egenmeldingsperioder.svar.serialisertTilString(),
            )
        }
        return null
    }

    private fun SykmeldingSporsmalSvarDto.riktigNarmesteLederSporsmalBuilder(
        sykmeldingId: String? = null,
        erAktivtArbidsforhold: Boolean? = null,
    ): SporsmalKafkaDTO? {
        if (erAktivtArbidsforhold == false) {
            logger.info(
                "Ber ikke om ny nærmeste leder for arbeidsforhold som ikke er aktivt: $sykmeldingId",
            )
            return SporsmalKafkaDTO(
                tekst = "Skal finne ny nærmeste leder",
                shortName = ShortNameKafkaDTO.NY_NARMESTE_LEDER,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar = "NEI",
            )
        }

        if (riktigNarmesteLeder != null) {
            return SporsmalKafkaDTO(
                tekst = riktigNarmesteLeder.sporsmaltekst,
                shortName = ShortNameKafkaDTO.NY_NARMESTE_LEDER,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar =
                    when (riktigNarmesteLeder.svar) {
                        JaEllerNei.JA -> JaEllerNei.NEI
                        JaEllerNei.NEI -> JaEllerNei.JA
                    }.name,
            )
        }
        return null
    }

    private fun SykmeldingSporsmalSvarDto.forsikringSporsmalBuilder(): SporsmalKafkaDTO? {
        if (harForsikring != null) {
            return SporsmalKafkaDTO(
                tekst = harForsikring.sporsmaltekst,
                shortName = ShortNameKafkaDTO.FORSIKRING,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar = harForsikring.svar.name,
            )
        }

        if (fisker?.blad?.svar == Blad.B && fisker.lottOgHyre.svar != LottOgHyre.HYRE) {
            return SporsmalKafkaDTO(
                tekst = "Har du forsikring som gjelder for de første 16 dagene av sykefraværet?",
                shortName = ShortNameKafkaDTO.FORSIKRING,
                svartype = SvartypeKafkaDTO.JA_NEI,
                svar = "JA",
            )
        }

        return null
    }
}
