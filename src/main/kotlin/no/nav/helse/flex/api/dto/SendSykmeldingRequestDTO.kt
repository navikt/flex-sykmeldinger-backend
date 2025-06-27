package no.nav.helse.flex.api.dto

import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.application.Egenmeldingsperiode
import java.time.LocalDate

fun JaEllerNei.tilBoolean(): Boolean = this == JaEllerNei.JA

data class SendSykmeldingRequestDTO(
    val erOpplysningeneRiktige: SporsmalSvar<JaEllerNei>,
    val arbeidssituasjon: SporsmalSvar<Arbeidssituasjon>,
    val arbeidsgiverOrgnummer: SporsmalSvar<String>?,
    val harEgenmeldingsdager: SporsmalSvar<JaEllerNei>? = null,
    val riktigNarmesteLeder: SporsmalSvar<JaEllerNei>? = null,
    val arbeidsledig: ArbeidsledigDTO? = null,
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
                    arbeidssituasjon = arbeidssituasjon,
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
                val arbeidsledigFraOrgnummer =
                    arbeidsledig?.arbeidsledigFraOrgnummer?.let { fraOrgnummer ->
                        SporsmalSvar(fraOrgnummer.sporsmaltekst, fraOrgnummer.svar)
                    }

                ArbeidsledigBrukerSvar(
                    arbeidssituasjon = arbeidssituasjon,
                    erOpplysningeneRiktige = SporsmalSvar(erOpplysningeneRiktige.sporsmaltekst, erOpplysningeneRiktige.svar.tilBoolean()),
                    arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                )
            }

            Arbeidssituasjon.PERMITTERT -> {
                val arbeidsledigFraOrgnummer =
                    arbeidsledig?.arbeidsledigFraOrgnummer?.let { fraOrgnummer ->
                        SporsmalSvar(fraOrgnummer.sporsmaltekst, fraOrgnummer.svar)
                    }

                PermittertBrukerSvar(
                    arbeidssituasjon = arbeidssituasjon,
                    erOpplysningeneRiktige = SporsmalSvar(erOpplysningeneRiktige.sporsmaltekst, erOpplysningeneRiktige.svar.tilBoolean()),
                    arbeidsledigFraOrgnummer = arbeidsledigFraOrgnummer,
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                )
            }

            Arbeidssituasjon.FISKER -> {
                requireNotNull(fisker) { "$arbeidssituasjon må ha satt fisker" }

                FiskerBrukerSvar(
                    arbeidssituasjon = SporsmalSvar(arbeidssituasjon.sporsmaltekst, arbeidssituasjon.svar),
                    erOpplysningeneRiktige =
                        SporsmalSvar(
                            erOpplysningeneRiktige.sporsmaltekst,
                            erOpplysningeneRiktige.svar.tilBoolean(),
                        ),
                    uriktigeOpplysninger =
                        uriktigeOpplysninger?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilUriktigeOpplysningerListe())
                        },
                    lottOgHyre =
                        SporsmalSvar(
                            fisker.lottOgHyre.sporsmaltekst,
                            fisker.lottOgHyre.svar.tilFiskerLottOgHyre(),
                        ),
                    blad =
                        SporsmalSvar(
                            fisker.blad.sporsmaltekst,
                            fisker.blad.svar.tilFiskerBlad(),
                        ),
                    arbeidsgiverOrgnummer =
                        arbeidsgiverOrgnummer?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar)
                        },
                    riktigNarmesteLeder =
                        riktigNarmesteLeder?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilBoolean())
                        },
                    harEgenmeldingsdager =
                        harEgenmeldingsdager?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar.tilBoolean())
                        },
                    egenmeldingsdager =
                        egenmeldingsdager?.let {
                            SporsmalSvar(it.sporsmaltekst, it.svar)
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

            Arbeidssituasjon.FRILANSER -> {
                FrilanserBrukerSvar(
                    arbeidssituasjon = arbeidssituasjon,
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
                    arbeidssituasjon = arbeidssituasjon,
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
                    arbeidssituasjon = arbeidssituasjon,
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
            Arbeidssituasjon.ANNET -> {
                AnnetArbeidssituasjonBrukerSvar(
                    arbeidssituasjon = arbeidssituasjon,
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
}

data class EgenmeldingsperiodeDTO(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class ArbeidsledigDTO(
    val arbeidsledigFraOrgnummer: SporsmalSvar<String>? = null,
)

data class FiskerDTO(
    val blad: SporsmalSvar<Blad>,
    val lottOgHyre: SporsmalSvar<LottOgHyre>,
)

enum class UriktigeOpplysningDTO {
    ANDRE_OPPLYSNINGER,
    ARBEIDSGIVER,
    DIAGNOSE,
    PERIODE,
    SYKMELDINGSGRAD_FOR_HOY,
    SYKMELDINGSGRAD_FOR_LAV,
}
