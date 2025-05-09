package no.nav.helse.flex.api.dto

import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.application.Egenmeldingsperiode
import java.time.LocalDate

data class SendSykmeldingRequestDTO(
    val erOpplysningeneRiktige: YesOrNoDTO,
    val arbeidssituasjon: Arbeidssituasjon,
    val arbeidsgiverOrgnummer: String? = null,
    val harEgenmeldingsdager: YesOrNoDTO? = null,
    val riktigNarmesteLeder: YesOrNoDTO? = null,
    val arbeidsledig: ArbeidsledigDTO? = null,
    val egenmeldingsdager: List<LocalDate>? = null,
    val egenmeldingsperioder: List<EgenmeldingsperiodeDTO>? = null,
    val fisker: FiskerDTO? = null,
    val harBruktEgenmelding: YesOrNoDTO? = null,
    val harForsikring: YesOrNoDTO? = null,
    val uriktigeOpplysninger: List<UriktigeOpplysningDTO>? = null,
) {
    fun tilBrukerSvar(): BrukerSvar =
        when (arbeidssituasjon) {
            Arbeidssituasjon.ARBEIDSTAKER -> {
                requireNotNull(arbeidsgiverOrgnummer) { "$arbeidssituasjon må ha satt arbeidsgiverOrgnummer" }
                ArbeidstakerBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean().somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer.somUkjentSporsmal(),
                    riktigNarmesteLeder = riktigNarmesteLeder?.tilBoolean()?.somUkjentSporsmal(),
                    harEgenmeldingsdager = harEgenmeldingsdager?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsdager = egenmeldingsdager?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.ARBEIDSLEDIG -> {
                requireNotNull(arbeidsledig) { "$arbeidssituasjon må ha satt arbeidsledig" }
                ArbeidsledigBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean().somUkjentSporsmal(),
                    arbeidsledigFraOrgnummer = arbeidsledig.arbeidsledigFraOrgnummer?.somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.PERMITTERT -> {
                requireNotNull(arbeidsledig) { "$arbeidssituasjon må ha satt arbeidsledig" }
                PermittertBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean().somUkjentSporsmal(),
                    arbeidsledigFraOrgnummer = arbeidsledig.arbeidsledigFraOrgnummer?.somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.FISKER -> {
                requireNotNull(fisker) { "$arbeidssituasjon må ha satt fisker" }
                FiskerBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean().somUkjentSporsmal(),
                    lottOgHyre = fisker.lottOgHyre.tilFiskerLottOgHyre().somUkjentSporsmal(),
                    blad = fisker.blad.tilFiskerBlad().somUkjentSporsmal(),
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer?.somUkjentSporsmal(),
                    riktigNarmesteLeder = riktigNarmesteLeder?.tilBoolean()?.somUkjentSporsmal(),
                    harEgenmeldingsdager = harEgenmeldingsdager?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsdager = egenmeldingsdager?.somUkjentSporsmal(),
                    harBruktEgenmelding = harBruktEgenmelding?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsperioder = egenmeldingsperioder?.tilEgenmeldingsperioder()?.somUkjentSporsmal(),
                    harForsikring = harForsikring?.tilBoolean()?.somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.FRILANSER -> {
                FrilanserBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean().somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                    harBruktEgenmelding = harBruktEgenmelding?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsperioder = egenmeldingsperioder?.tilEgenmeldingsperioder()?.somUkjentSporsmal(),
                    harForsikring = harForsikring?.tilBoolean()?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.NAERINGSDRIVENDE -> {
                NaringsdrivendeBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean().somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                    harBruktEgenmelding = harBruktEgenmelding?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsperioder = egenmeldingsperioder?.tilEgenmeldingsperioder()?.somUkjentSporsmal(),
                    harForsikring = harForsikring?.tilBoolean()?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.JORDBRUKER -> {
                JordbrukerBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean().somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                    harBruktEgenmelding = harBruktEgenmelding?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsperioder = egenmeldingsperioder?.tilEgenmeldingsperioder()?.somUkjentSporsmal(),
                    harForsikring = harForsikring?.tilBoolean()?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.ANNET -> {
                AnnetArbeidssituasjonBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean().somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                )
            }
        }

    private fun List<UriktigeOpplysningDTO>.tilUriktigeOpplysningerListe(): List<UriktigeOpplysning> =
        this.map { it.tilUriktigeOpplysninger() }

    private fun UriktigeOpplysningDTO.tilUriktigeOpplysninger(): UriktigeOpplysning =
        when (this) {
            UriktigeOpplysningDTO.PERIODE -> UriktigeOpplysning.PERIODE
            UriktigeOpplysningDTO.ANDRE_OPPLYSNINGER -> UriktigeOpplysning.ANDRE_OPPLYSNINGER
            UriktigeOpplysningDTO.ARBEIDSGIVER -> UriktigeOpplysning.ARBEIDSGIVER
            UriktigeOpplysningDTO.DIAGNOSE -> UriktigeOpplysning.DIAGNOSE
            UriktigeOpplysningDTO.SYKMELDINGSGRAD_FOR_HOY -> UriktigeOpplysning.SYKMELDINGSGRAD_FOR_HOY
            UriktigeOpplysningDTO.SYKMELDINGSGRAD_FOR_LAV -> UriktigeOpplysning.SYKMELDINGSGRAD_FOR_LAV
        }

    private fun List<EgenmeldingsperiodeDTO>.tilEgenmeldingsperioder(): List<Egenmeldingsperiode> =
        this.map {
            requireNotNull(it.fom) { "Fom (fra og med) er ikke satt på egenmeldingsperiode: $it" }
            requireNotNull(it.tom) { "Tom (til og med) er ikke satt på egenmeldingsperiode: $it" }
            Egenmeldingsperiode(fom = it.fom, tom = it.tom)
        }

    private fun YesOrNoDTO.tilBoolean(): Boolean = this == YesOrNoDTO.YES

    private fun LottOgHyre.tilFiskerLottOgHyre(): FiskerLottOgHyre =
        when (this) {
            LottOgHyre.LOTT -> FiskerLottOgHyre.LOTT
            LottOgHyre.HYRE -> FiskerLottOgHyre.HYRE
            LottOgHyre.BEGGE -> FiskerLottOgHyre.BEGGE
        }

    private fun Blad.tilFiskerBlad(): FiskerBlad =
        when (this) {
            Blad.A -> FiskerBlad.A
            Blad.B -> FiskerBlad.B
        }

    private fun <T : Any> T.somUkjentSporsmal(): SporsmalSvar<T> = SporsmalSvar("<ukjent sporsmal>", this)
}

enum class YesOrNoDTO {
    YES,
    NO,
}

data class EgenmeldingsperiodeDTO(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class ArbeidsledigDTO(
    val arbeidsledigFraOrgnummer: String? = null,
)

data class FiskerDTO(
    val blad: Blad,
    val lottOgHyre: LottOgHyre,
)

enum class UriktigeOpplysningDTO {
    ANDRE_OPPLYSNINGER,
    ARBEIDSGIVER,
    DIAGNOSE,
    PERIODE,
    SYKMELDINGSGRAD_FOR_HOY,
    SYKMELDINGSGRAD_FOR_LAV,
}
