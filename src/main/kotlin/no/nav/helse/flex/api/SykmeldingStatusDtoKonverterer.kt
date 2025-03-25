package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.api.dto.SykmeldingStatusDTO
import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.application.Egenmeldingsperiode
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.SykmeldingHendelse
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class SykmeldingStatusDtoKonverterer {
    fun konverterSykmeldingStatus(hendelse: SykmeldingHendelse): SykmeldingStatusDTO =
        SykmeldingStatusDTO(
            // TODO
            statusEvent = konverterHendelseStatus(hendelse.status),
            timestamp = hendelse.opprettet.atOffset(ZoneOffset.UTC),
            sporsmalOgSvarListe = emptyList(),
            arbeidsgiver =
                hendelse.arbeidstakerInfo?.arbeidsgiver?.let { arbeidsgiver ->
                    ArbeidsgiverStatusDTO(
                        orgnummer = arbeidsgiver.orgnummer,
                        juridiskOrgnummer = arbeidsgiver.juridiskOrgnummer,
                        orgNavn = arbeidsgiver.orgnavn,
                    )
                },
            // TODO
            brukerSvar = null, // hendelse.sporsmalSvar?.let { konverterSykmeldingSporsmal(it) },
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

    internal fun konverterSykmeldingSporsmalSvar(brukerSvar: BrukerSvar): SykmeldingSporsmalSvarDto =
        when (brukerSvar) {
            is ArbeidstakerBrukerSvar -> SykmeldingSporsmalSvarDto(
                erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                arbeidssituasjon = brukerSvar.arbeidssituasjonSporsmal.tilArbeidssituasjonFormSvar(),
                arbeidsgiverOrgnummer = brukerSvar.arbeidsgiverOrgnummer.tilFritekstFormSvar(),
                riktigNarmesteLeder = brukerSvar.riktigNarmesteLeder.tilJaEllerNeiFormSvar(),
                egenmeldingsdager = brukerSvar.egenmeldingsdager?.tilDatolisteFormSvar(),
                harBruktEgenmeldingsdager = brukerSvar.harEgenmeldingsdager.tilJaEllerNeiFormSvar(),
            )

            is AnnetArbeidssituasjonBrukerSvar -> SykmeldingSporsmalSvarDto(
                erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                arbeidssituasjon = brukerSvar.arbeidssituasjonSporsmal.tilArbeidssituasjonFormSvar(),
            )
            is ArbeidsledigBrukerSvar -> TODO()
            is FiskerBrukerSvar -> TODO()
            is FrilanserBrukerSvar -> SykmeldingSporsmalSvarDto(
                erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
                uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
                arbeidssituasjon = brukerSvar.arbeidssituasjonSporsmal.tilArbeidssituasjonFormSvar(),
                harBruktEgenmelding = brukerSvar.harBruktEgenmelding.tilJaEllerNeiFormSvar(),
                egenmeldingsperioder =  : SporsmalSvar<List<Egenmeldingsperiode>>? = null,
            val harForsikring: SporsmalSvar<Boolean>,
        override val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysning>>? = null,
            )
            is JordbrukerBrukerSvar -> SykmeldingSporsmalSvarDto(
//                erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
//                uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
//                arbeidssituasjon = brukerSvar.arbeidssituasjonSporsmal.tilArbeidssituasjonFormSvar(),
            )
            is NaringsdrivendeBrukerSvar -> SykmeldingSporsmalSvarDto(
//                erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
//                uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
//                arbeidssituasjon = brukerSvar.arbeidssituasjonSporsmal.tilArbeidssituasjonFormSvar(),
            )
            is PermittertBrukerSvar -> TODO()
        }
        SykmeldingSporsmalSvarDto(
            erOpplysningeneRiktige = brukerSvar.erOpplysningeneRiktige.tilJaEllerNeiFormSvar(),
            uriktigeOpplysninger = brukerSvar.uriktigeOpplysninger?.tilUriktigeOpplysningerFormSvar(),
            arbeidssituasjon = brukerSvar.arbeidssituasjonSporsmal.tilArbeidssituasjonFormSvar(),
            arbeidsgiverOrgnummer = brukerSvar.ar,
            arbeidsledig = TODO(),
            riktigNarmesteLeder = TODO(),
            harBruktEgenmelding = TODO(),
            egenmeldingsperioder = TODO(),
            harForsikring = TODO(),
            egenmeldingsdager = TODO(),
            harBruktEgenmeldingsdager = TODO(),
            fisker = TODO(),
        )

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
            svar = svar
        )

    private fun SporsmalSvar<List<LocalDate>>.tilDatolisteFormSvar(): FormSporsmalSvar<List<LocalDate>> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar = svar
        )

    private fun SporsmalSvar<List<Egenmeldingsperiode>>.tilEgenmeldingsperioderFormSvar(): FormSporsmalSvar<List<EgenmeldingsperiodeDTO>> =
        FormSporsmalSvar(
            sporsmaltekst = sporsmaltekst,
            svar = svar.map { EgenmeldingsperiodeDTO(it.fom, it.tom) }
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

    private fun Boolean.tilJaEllerNei() =
        when (this) {
            true -> JaEllerNei.JA
            false -> JaEllerNei.NEI
        }

    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    )
}
