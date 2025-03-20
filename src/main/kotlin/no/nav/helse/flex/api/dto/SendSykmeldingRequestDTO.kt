package no.nav.helse.flex.api.dto

import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.application.Egenmeldingsperiode
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.utils.objectMapper
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
                requireNotNull(riktigNarmesteLeder) { "$arbeidssituasjon må ha satt riktigNarmesteLeder" }
                requireNotNull(harEgenmeldingsdager) { "$arbeidssituasjon må ha satt harEgenmeldingsdager" }
                ArbeidstakerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe(),
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                    riktigNarmesteLeder = riktigNarmesteLeder.tilBoolean(),
                    harEgenmeldingsdager = harEgenmeldingsdager.tilBoolean(),
                    egenmeldingsdager = egenmeldingsdager,
                )
            }
            Arbeidssituasjon.ARBEIDSLEDIG -> {
                requireNotNull(arbeidsledig) { "$arbeidssituasjon må ha satt arbeidsledig" }
                requireNotNull(arbeidsledig.arbeidsledigFraOrgnummer) { "$arbeidssituasjon må ha satt arbeidsledigFraOrgnummer" }
                ArbeidsledigBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean(),
                    arbeidsledigFraOrgnummer = arbeidsledig.arbeidsledigFraOrgnummer,
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe(),
                )
            }
            Arbeidssituasjon.PERMITTERT -> {
                requireNotNull(arbeidsledig) { "$arbeidssituasjon må ha satt arbeidsledig" }
                requireNotNull(arbeidsledig.arbeidsledigFraOrgnummer) { "$arbeidssituasjon må ha satt arbeidsledigFraOrgnummer" }
                PermittertBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean(),
                    arbeidsledigFraOrgnummer = arbeidsledig.arbeidsledigFraOrgnummer,
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe(),
                )
            }
            Arbeidssituasjon.FISKER -> {
                requireNotNull(fisker) { "$arbeidssituasjon må ha satt fisker" }
                FiskerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean(),
                    lottOgHyre = fisker.lottOgHyre.tilFiskerLottOgHyre(),
                    blad = fisker.blad.tilFiskerBlad(),
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                    riktigNarmesteLeder = riktigNarmesteLeder?.tilBoolean(),
                    harEgenmeldingsdager = harEgenmeldingsdager?.tilBoolean(),
                    egenmeldingsdager = egenmeldingsdager,
                    harBruktEgenmelding = harBruktEgenmelding?.tilBoolean(),
                    egenmeldingsperioder = egenmeldingsperioder?.tilEgenmeldingsperioder(),
                    harForsikring = harForsikring?.tilBoolean(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe(),
                )
            }
            Arbeidssituasjon.FRILANSER -> {
                requireNotNull(harBruktEgenmelding) { "$arbeidssituasjon må ha satt harBruktEgenmelding" }
                requireNotNull(harForsikring) { "$arbeidssituasjon må ha satt harForsikring" }
                FrilanserBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe(),
                    harBruktEgenmelding = harBruktEgenmelding.tilBoolean(),
                    egenmeldingsperioder = egenmeldingsperioder?.tilEgenmeldingsperioder(),
                    harForsikring = harForsikring.tilBoolean(),
                )
            }
            Arbeidssituasjon.NAERINGSDRIVENDE -> {
                requireNotNull(harBruktEgenmelding) { "$arbeidssituasjon må ha satt harBruktEgenmelding" }
                requireNotNull(harForsikring) { "$arbeidssituasjon må ha satt harForsikring" }
                NaringsdrivendeBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe(),
                    harBruktEgenmelding = harBruktEgenmelding.tilBoolean(),
                    egenmeldingsperioder = egenmeldingsperioder?.tilEgenmeldingsperioder(),
                    harForsikring = harForsikring.tilBoolean(),
                )
            }
            Arbeidssituasjon.JORDBRUKER -> {
                requireNotNull(harBruktEgenmelding) { "$arbeidssituasjon må ha satt harBruktEgenmelding" }
                requireNotNull(harForsikring) { "$arbeidssituasjon må ha satt harForsikring" }
                JordbrukerBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe(),
                    harBruktEgenmelding = harBruktEgenmelding.tilBoolean(),
                    egenmeldingsperioder = egenmeldingsperioder?.tilEgenmeldingsperioder(),
                    harForsikring = harForsikring.tilBoolean(),
                )
            }
            Arbeidssituasjon.ANNET -> {
                AnnetArbeidssituasjonBrukerSvar(
                    erOpplysningeneRiktige = erOpplysningeneRiktige.tilBoolean(),
                    uriktigeOpplysninger = uriktigeOpplysninger?.tilUriktigeOpplysningerListe(),
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

    fun List<EgenmeldingsperiodeDTO>.tilEgenmeldingsperioder(): List<Egenmeldingsperiode> =
        this.map { Egenmeldingsperiode(fom = it.fom, tom = it.tom) }

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

    fun tilSporsmalListe(): List<Sporsmal> {
        val sporsmal = mutableListOf<Sporsmal>()
        erOpplysningeneRiktige.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
                    svartype = Svartype.JA_NEI,
                    svar = konverterJaNeiSvar(it),
                ),
            )
        }
        arbeidsgiverOrgnummer?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
                    svartype = Svartype.RADIO,
                    svar = listOf(Svar(verdi = it)),
                ),
            )
        }
        arbeidssituasjon.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSSITUASJON,
                    svartype = Svartype.RADIO,
                    svar = listOf(Svar(verdi = it.name)),
                ),
            )
        }
        riktigNarmesteLeder?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.RIKTIG_NARMESTE_LEDER,
                    svartype = Svartype.JA_NEI,
                    svar = konverterJaNeiSvar(it),
                ),
            )
        }
        arbeidsledig?.arbeidsledigFraOrgnummer?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSLEDIG_FRA_ORGNUMMER,
                    svartype = Svartype.RADIO,
                    svar = listOf(Svar(verdi = it)),
                ),
            )
        }
        // Brukes for arbeidstaker og fisker på hyre
        egenmeldingsdager?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.EGENMELDINGSDAGER,
                    svartype = Svartype.DATOER,
                    svar = it.map { dag -> Svar(verdi = dag.toString()) },
                ),
            )
        }
        // Brukes for selvstendig næringsdrivende, frilanser, jordbruker og fisker på lott
        egenmeldingsperioder?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.EGENMELDINGSPERIODER,
                    svartype = Svartype.PERIODER,
                    svar = it.map { periode -> Svar(verdi = objectMapper.writeValueAsString(periode)) },
                ),
            )
        }
        fisker?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.FISKER,
                    svartype = Svartype.GRUPPE_AV_UNDERSPORSMAL,
                    svar = emptyList(),
                    undersporsmal =
                        listOf(
                            Sporsmal(
                                tag = SporsmalTag.FISKER__BLAD,
                                svartype = Svartype.RADIO,
                                svar = listOf(Svar(verdi = it.blad.name)),
                            ),
                            Sporsmal(
                                tag = SporsmalTag.FISKER__LOTT_OG_HYRE,
                                svartype = Svartype.RADIO,
                                svar = listOf(Svar(verdi = it.lottOgHyre.name)),
                            ),
                        ),
                ),
            )
        }
        harBruktEgenmelding?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
                    svartype = Svartype.JA_NEI,
                    svar = konverterJaNeiSvar(it),
                ),
            )
        }
        harEgenmeldingsdager?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDINGSDAGER,
                    svartype = Svartype.JA_NEI,
                    svar = konverterJaNeiSvar(it),
                ),
            )
        }
        harForsikring?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.HAR_FORSIKRING,
                    svartype = Svartype.JA_NEI,
                    svar = konverterJaNeiSvar(it),
                ),
            )
        }
        uriktigeOpplysninger?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.URIKTIGE_OPPLYSNINGER,
                    svartype = Svartype.CHECKBOX_GRUPPE,
                    svar = it.map { opplysning -> Svar(verdi = opplysning.name) },
                ),
            )
        }
        return sporsmal
    }

    private fun konverterJaNeiSvar(svar: YesOrNoDTO?): List<Svar> =
        when (svar) {
            YesOrNoDTO.YES -> listOf(Svar(verdi = "JA"))
            YesOrNoDTO.NO -> listOf(Svar(verdi = "NEI"))
            else -> emptyList()
        }
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
