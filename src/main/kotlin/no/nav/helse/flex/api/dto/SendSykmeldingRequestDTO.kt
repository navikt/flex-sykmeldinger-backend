package no.nav.helse.flex.api.dto

import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.application.Egenmeldingsperiode
import java.time.LocalDate

/*

data class SykmeldingFormResponse(
    val erOpplysningeneRiktige: SporsmalSvar<JaEllerNei>,
    val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysningerType>>?,
    val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>?,
    val arbeidsledig: ArbeidsledigFraOrgnummer?,
    val riktigNarmesteLeder: SporsmalSvar<JaEllerNei>?,
    val harBruktEgenmelding: SporsmalSvar<JaEllerNei>?,
    val egenmeldingsperioder: SporsmalSvar<List<Egenmeldingsperiode>>?,
    val harForsikring: SporsmalSvar<JaEllerNei>?,
    val egenmeldingsdager: SporsmalSvar<List<LocalDate>>?,
    val harBruktEgenmeldingsdager: SporsmalSvar<JaEllerNei>?,
    val fisker: FiskerSvar?,
)

Internal server error - JSON parse error: Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`) - POST: /api/v1/sykmeldinger/609e84cc-1435-46b1-a478-42309838e70a/send



 */

fun JaEllerNei.tilBoolean(): Boolean = this == JaEllerNei.JA

data class SendSykmeldingRequestDTO(
    val erOpplysningeneRiktige: SporsmalSvar<JaEllerNei>,
    val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>?,
    val harEgenmeldingsdager: SporsmalSvar<JaEllerNei>? = null,
    val riktigNarmesteLeder: SporsmalSvar<JaEllerNei>? = null,
    val arbeidsledig: ArbeidsledigDTO? = null, // Changed from SporsmalSvar<ArbeidsledigDTO>?
    val egenmeldingsdager: SporsmalSvar<List<LocalDate>>? = null,
    val egenmeldingsperioder: SporsmalSvar<List<EgenmeldingsperiodeDTO>>? = null,
    val fisker: FiskerDTO? = null,
    val harBruktEgenmelding: SporsmalSvar<JaEllerNei>? = null,
    val harForsikring: SporsmalSvar<JaEllerNei>? = null,
    val uriktigeOpplysninger: SporsmalSvar<List<UriktigeOpplysningDTO>>? = null,
) {
    fun tilBrukerSvar(): BrukerSvar =
        when (arbeidssituasjon.svar) {
            Arbeidssituasjon.ARBEIDSTAKER -> {
                requireNotNull(arbeidsgiverOrgnummer) { "$arbeidssituasjon må ha satt arbeidsgiverOrgnummer" }
                ArbeidstakerBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    erOpplysningeneRiktige = SporsmalSvar(erOpplysningeneRiktige.sporsmaltekst, erOpplysningeneRiktige.svar.tilBoolean()),
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                    arbeidsgiverOrgnummer = SporsmalSvar(arbeidsgiverOrgnummer.sporsmaltekst, arbeidsgiverOrgnummer.svar),
                    riktigNarmesteLeder = riktigNarmesteLeder?.let { SporsmalSvar(it.sporsmaltekst, it.svar.tilBoolean()) },
                    harEgenmeldingsdager = harEgenmeldingsdager?.let { SporsmalSvar(it.sporsmaltekst, it.svar.tilBoolean()) },
                    egenmeldingsdager = egenmeldingsdager?.let { SporsmalSvar(it.sporsmaltekst, it.svar) },
                )
            }
            Arbeidssituasjon.ARBEIDSLEDIG -> {
                requireNotNull(arbeidsledig) { "$arbeidssituasjon må ha satt arbeidsledig (ArbeidsledigDTO details)" }
                requireNotNull(arbeidsledig.arbeidsledigFraOrgnummer)
                val arbeidsledigFra = arbeidsledig.arbeidsledigFraOrgnummer.svar
                val sporsmalOmArbeidsledigFraOrgnummer = arbeidsledig.arbeidsledigFraOrgnummer.sporsmaltekst

                // error out if arbeidsledigFraOrgnummer is null

                ArbeidsledigBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    erOpplysningeneRiktige = SporsmalSvar(erOpplysningeneRiktige.sporsmaltekst, erOpplysningeneRiktige.svar.tilBoolean()),
                    arbeidsledigFraOrgnummer = SporsmalSvar(sporsmalOmArbeidsledigFraOrgnummer, arbeidsledigFra),
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                )
            }
            Arbeidssituasjon.PERMITTERT -> {
                requireNotNull(arbeidsledig) { "$arbeidssituasjon må ha satt arbeidsledig (ArbeidsledigDTO details)" }

                requireNotNull(arbeidsledig) { "$arbeidssituasjon må ha satt arbeidsledig (ArbeidsledigDTO details)" }
                requireNotNull(arbeidsledig.arbeidsledigFraOrgnummer)
                val arbeidsledigFra = arbeidsledig.arbeidsledigFraOrgnummer.svar
                val sporsmalOmArbeidsledigFraOrgnummer = arbeidsledig.arbeidsledigFraOrgnummer.sporsmaltekst

                PermittertBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    erOpplysningeneRiktige = SporsmalSvar(erOpplysningeneRiktige.sporsmaltekst, erOpplysningeneRiktige.svar.tilBoolean()),
                    arbeidsledigFraOrgnummer = SporsmalSvar(sporsmalOmArbeidsledigFraOrgnummer, arbeidsledigFra),
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                )
            }
            Arbeidssituasjon.FISKER -> {
                requireNotNull(fisker) { "$arbeidssituasjon må ha satt fisker" }
                FiskerBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon.svar.somUkjentSporsmal(),
                    erOpplysningeneRiktige = erOpplysningeneRiktige.svar.tilBoolean().somUkjentSporsmal(),
                    lottOgHyre = fisker.lottOgHyre.tilFiskerLottOgHyre().somUkjentSporsmal(),
                    blad = fisker.blad.tilFiskerBlad().somUkjentSporsmal(),
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer?.svar?.somUkjentSporsmal(),
                    riktigNarmesteLeder = riktigNarmesteLeder?.svar?.tilBoolean()?.somUkjentSporsmal(),
                    harEgenmeldingsdager = harEgenmeldingsdager?.svar?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsdager = egenmeldingsdager?.svar?.somUkjentSporsmal(),
                    harBruktEgenmelding = harBruktEgenmelding?.svar?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsperioder = egenmeldingsperioder?.svar?.tilEgenmeldingsperioder()?.somUkjentSporsmal(),
                    harForsikring = harForsikring?.svar?.tilBoolean()?.somUkjentSporsmal(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.svar?.tilUriktigeOpplysningerListe()?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.FRILANSER -> {
                FrilanserBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            erOpplysningeneRiktige.sporsmaltekst,
                            erOpplysningeneRiktige.svar.tilBoolean(),
                        ),
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                    harBruktEgenmelding =
                        harBruktEgenmelding?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilBoolean())
                        },
                    egenmeldingsperioder =
                        egenmeldingsperioder?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilEgenmeldingsperioder())
                        },
                    harForsikring =
                        harForsikring?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilBoolean())
                        },
                )
            }
            Arbeidssituasjon.NAERINGSDRIVENDE -> {
                NaringsdrivendeBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    erOpplysningeneRiktige = SporsmalSvar(erOpplysningeneRiktige.sporsmaltekst, erOpplysningeneRiktige.svar.tilBoolean()),
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                    harBruktEgenmelding = harBruktEgenmelding?.let { SporsmalSvar(it.sporsmaltekst, it.svar.tilBoolean()) },
                    egenmeldingsperioder =
                        egenmeldingsperioder?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilEgenmeldingsperioder())
                        },
                    harForsikring = harForsikring?.let { SporsmalSvar(harForsikring.sporsmaltekst, harForsikring.svar.tilBoolean()) },
                )
            }
            Arbeidssituasjon.JORDBRUKER -> {
                JordbrukerBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    erOpplysningeneRiktige = SporsmalSvar(erOpplysningeneRiktige.sporsmaltekst, erOpplysningeneRiktige.svar.tilBoolean()),
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                    harBruktEgenmelding = harBruktEgenmelding?.svar?.tilBoolean()?.somUkjentSporsmal(),
                    egenmeldingsperioder = egenmeldingsperioder?.svar?.tilEgenmeldingsperioder()?.somUkjentSporsmal(),
                    harForsikring = harForsikring?.svar?.tilBoolean()?.somUkjentSporsmal(),
                )
            }
            Arbeidssituasjon.ANNET -> {
                AnnetArbeidssituasjonBrukerSvar(
                    arbeidssituasjonSporsmal = arbeidssituasjon,
                    erOpplysningeneRiktige = SporsmalSvar(erOpplysningeneRiktige.sporsmaltekst, erOpplysningeneRiktige.svar.tilBoolean()),
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
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
    val arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
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
