package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.api.dto.SykmeldingStatusDTO
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.application.Egenmeldingsperiode
import no.nav.helse.flex.sykmeldinghendelse.*
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class SykmeldingStatusDtoKonverterer {
    fun konverterSykmeldingStatus(hendelse: SykmeldingHendelse): SykmeldingStatusDTO =
        SykmeldingStatusDTO(
            statusEvent = konverterHendelseStatus(hendelse.status),
            timestamp = hendelse.hendelseOpprettet.atOffset(ZoneOffset.UTC),
            // TODO
            arbeidsgiver = hendelse.tilleggsinfo?.let(::konverterArbeidsgiver),
            brukerSvar = hendelse.brukerSvar?.let { konverterSykmeldingSporsmalSvarForBruker(it) },
        )

    fun konverterSykmeldingSporsmalSvar(brukerSvar: BrukerSvar): SykmeldingSporsmalSvarDto =
        konverterAlleSykmeldingSporsmalSvar(
            brukerSvar = brukerSvar,
        )

    private fun konverterHendelseStatus(status: HendelseStatus): String =
        when (status) {
            HendelseStatus.APEN -> "APEN"
            HendelseStatus.SENDT_TIL_ARBEIDSGIVER -> "SENDT"
            HendelseStatus.SENDT_TIL_NAV -> "BEKREFTET"
            HendelseStatus.AVBRUTT -> "AVBRUTT"
            HendelseStatus.UTGATT -> "UTGATT"
            HendelseStatus.BEKREFTET_AVVIST -> "BEKREFTET"
        }

    internal fun konverterArbeidsgiver(tilleggsinfo: Tilleggsinfo): ArbeidsgiverStatusDTO? =
        when (tilleggsinfo) {
            is ArbeidstakerTilleggsinfo -> konverterArbeidsgiver(tilleggsinfo.arbeidsgiver)
            is FiskerTilleggsinfo -> tilleggsinfo.arbeidsgiver?.let(::konverterArbeidsgiver)
            is UtdatertFormatTilleggsinfo -> tilleggsinfo.arbeidsgiver?.let(::konverterUtdatertFormatArbeidsgiver)
            else -> null
        }

    private fun konverterArbeidsgiver(arbeidsgiver: Arbeidsgiver): ArbeidsgiverStatusDTO =
        ArbeidsgiverStatusDTO(
            orgnummer = arbeidsgiver.orgnummer,
            juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
            orgNavn = arbeidsgiver.orgnavn,
        )

    private fun konverterUtdatertFormatArbeidsgiver(arbeidsgiver: UtdatertFormatArbeidsgiver): ArbeidsgiverStatusDTO =
        ArbeidsgiverStatusDTO(
            orgnummer = arbeidsgiver.orgnummer,
            juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
            orgNavn = arbeidsgiver.orgnavn,
        )

    private fun konverterSykmeldingSporsmalSvarForBruker(brukerSvar: BrukerSvar): SykmeldingSporsmalSvarDto? =
        when (brukerSvar) {
            is UtdatertFormatBrukerSvar -> null
            else -> konverterSykmeldingSporsmalSvar(brukerSvar)
        }

    private fun konverterAlleSykmeldingSporsmalSvar(brukerSvar: BrukerSvar): SykmeldingSporsmalSvarDto =
        when (brukerSvar) {
            is ArbeidstakerBrukerSvar ->
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                    arbeidssituasjon = brukerSvar.arbeidssituasjon.tilArbeidssituasjonFormSvar(),
                    uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                    arbeidsgiverOrgnummer = brukerSvar.arbeidsgiverOrgnummer.tilFritekstFormSvar(),
                    riktigNarmesteLeder = brukerSvar.riktigNarmesteLeder?.tilJaEllerNeiFormSvar(),
                    egenmeldingsdager = brukerSvar.egenmeldingsdager?.tilDatolisteFormSvar(),
                    harBruktEgenmeldingsdager = brukerSvar.harEgenmeldingsdager?.tilJaEllerNeiFormSvar(),
                )

            is AnnetArbeidssituasjonBrukerSvar ->
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                    arbeidssituasjon = brukerSvar.arbeidssituasjon.tilArbeidssituasjonFormSvar(),
                    uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                )
            is ArbeidsledigBrukerSvar ->
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                    arbeidssituasjon = brukerSvar.arbeidssituasjon.tilArbeidssituasjonFormSvar(),
                    arbeidsgiverOrgnummer = brukerSvar.arbeidsledigFraOrgnummer?.tilFritekstFormSvar(),
                    uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                )
            is PermittertBrukerSvar ->
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                    arbeidssituasjon = brukerSvar.arbeidssituasjon.tilArbeidssituasjonFormSvar(),
                    arbeidsgiverOrgnummer = brukerSvar.arbeidsledigFraOrgnummer?.tilFritekstFormSvar(),
                    uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                )
            is FiskerBrukerSvar ->
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                    uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                    arbeidssituasjon = brukerSvar.arbeidssituasjon.tilArbeidssituasjonFormSvar(),
                    arbeidsgiverOrgnummer = brukerSvar.arbeidsgiverOrgnummer?.tilFritekstFormSvar(),
                    riktigNarmesteLeder = brukerSvar.riktigNarmesteLeder?.tilJaEllerNeiFormSvar(),
                    egenmeldingsdager = brukerSvar.egenmeldingsdager?.tilDatolisteFormSvar(),
                    harBruktEgenmeldingsdager = brukerSvar.harEgenmeldingsdager?.tilJaEllerNeiFormSvar(),
                    harBruktEgenmelding = brukerSvar.harBruktEgenmelding?.tilJaEllerNeiFormSvar(),
                    egenmeldingsperioder = brukerSvar.egenmeldingsperioder?.tilEgenmeldingsperioderFormSvar(),
                    harForsikring = brukerSvar.harForsikring?.tilJaEllerNeiFormSvar(),
                    fisker =
                        FiskerSvar(
                            blad = brukerSvar.blad.tilFiskerBladFormSvar(),
                            lottOgHyre = brukerSvar.lottOgHyre.tilFiskerLottOgHyreFormSvar(),
                        ),
                )
            is FrilanserBrukerSvar ->
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                    uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                    arbeidssituasjon = brukerSvar.arbeidssituasjon.tilArbeidssituasjonFormSvar(),
                    harBruktEgenmelding = brukerSvar.harBruktEgenmelding?.tilJaEllerNeiFormSvar(),
                    egenmeldingsperioder = brukerSvar.egenmeldingsperioder?.tilEgenmeldingsperioderFormSvar(),
                    harForsikring = brukerSvar.harForsikring?.tilJaEllerNeiFormSvar(),
                )
            is JordbrukerBrukerSvar ->
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                    uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                    arbeidssituasjon = brukerSvar.arbeidssituasjon.tilArbeidssituasjonFormSvar(),
                    harBruktEgenmelding = brukerSvar.harBruktEgenmelding?.tilJaEllerNeiFormSvar(),
                    egenmeldingsperioder = brukerSvar.egenmeldingsperioder?.tilEgenmeldingsperioderFormSvar(),
                    harForsikring = brukerSvar.harForsikring?.tilJaEllerNeiFormSvar(),
                )
            is NaringsdrivendeBrukerSvar ->
                SykmeldingSporsmalSvarDto(
                    erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                    uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                    arbeidssituasjon = brukerSvar.arbeidssituasjon.tilArbeidssituasjonFormSvar(),
                    harBruktEgenmelding = brukerSvar.harBruktEgenmelding?.tilJaEllerNeiFormSvar(),
                    egenmeldingsperioder = brukerSvar.egenmeldingsperioder?.tilEgenmeldingsperioderFormSvar(),
                    harForsikring = brukerSvar.harForsikring?.tilJaEllerNeiFormSvar(),
                )
            is UtdatertFormatBrukerSvar -> error("Ikke implementert for UtdatertFormatBrukerSvar")
        }

    private fun SporsmalSvar<Boolean>.tilJaEllerNeiFormSvar(): FormSporsmalSvar<JaEllerNei> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar =
                when (svar) {
                    true -> JaEllerNei.JA
                    false -> JaEllerNei.NEI
                },
        )

    private fun SporsmalSvar<String>.tilFritekstFormSvar(): FormSporsmalSvar<String> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar = svar,
        )

    private fun SporsmalSvar<List<LocalDate>>.tilDatolisteFormSvar(): FormSporsmalSvar<List<LocalDate>> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar = svar,
        )

    private fun SporsmalSvar<List<Egenmeldingsperiode>>.tilEgenmeldingsperioderFormSvar(): FormSporsmalSvar<
        List<EgenmeldingsperiodeFormDTO>,
    > =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar = svar.map { EgenmeldingsperiodeFormDTO(it.fom, it.tom) },
        )

    private fun SporsmalSvar<Arbeidssituasjon>.tilArbeidssituasjonFormSvar(): FormSporsmalSvar<ArbeidssituasjonDTO> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar =
                when (svar) {
                    Arbeidssituasjon.ARBEIDSTAKER -> ArbeidssituasjonDTO.ARBEIDSTAKER
                    Arbeidssituasjon.ARBEIDSLEDIG -> ArbeidssituasjonDTO.ARBEIDSLEDIG
                    Arbeidssituasjon.PERMITTERT -> ArbeidssituasjonDTO.PERMITTERT
                    Arbeidssituasjon.FISKER -> ArbeidssituasjonDTO.FISKER
                    Arbeidssituasjon.FRILANSER -> ArbeidssituasjonDTO.FRILANSER
                    Arbeidssituasjon.NAERINGSDRIVENDE -> ArbeidssituasjonDTO.NAERINGSDRIVENDE
                    Arbeidssituasjon.JORDBRUKER -> ArbeidssituasjonDTO.JORDBRUKER
                    Arbeidssituasjon.ANNET -> ArbeidssituasjonDTO.ANNET
                },
        )

    private fun SporsmalSvar<List<UriktigeOpplysning>>.tilUriktigeOpplysningerFormSvar(): FormSporsmalSvar<List<UriktigeOpplysningerType>> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar =
                svar.map {
                    when (it) {
                        UriktigeOpplysning.ANDRE_OPPLYSNINGER -> UriktigeOpplysningerType.ANDRE_OPPLYSNINGER
                        UriktigeOpplysning.ARBEIDSGIVER -> UriktigeOpplysningerType.ARBEIDSGIVER
                        UriktigeOpplysning.DIAGNOSE -> UriktigeOpplysningerType.DIAGNOSE
                        UriktigeOpplysning.PERIODE -> UriktigeOpplysningerType.PERIODE
                        UriktigeOpplysning.SYKMELDINGSGRAD_FOR_HOY -> UriktigeOpplysningerType.SYKMELDINGSGRAD_FOR_HOY
                        UriktigeOpplysning.SYKMELDINGSGRAD_FOR_LAV -> UriktigeOpplysningerType.SYKMELDINGSGRAD_FOR_LAV
                    }
                },
        )

    private fun SporsmalSvar<FiskerBlad>.tilFiskerBladFormSvar(): FormSporsmalSvar<Blad> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar =
                when (svar) {
                    FiskerBlad.A -> Blad.A
                    FiskerBlad.B -> Blad.B
                },
        )

    private fun SporsmalSvar<FiskerLottOgHyre>.tilFiskerLottOgHyreFormSvar(): FormSporsmalSvar<LottOgHyre> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar =
                when (svar) {
                    FiskerLottOgHyre.LOTT -> LottOgHyre.LOTT
                    FiskerLottOgHyre.HYRE -> LottOgHyre.HYRE
                    FiskerLottOgHyre.BEGGE -> LottOgHyre.BEGGE
                },
        )
}
