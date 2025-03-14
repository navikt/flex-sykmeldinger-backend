package no.nav.helse.flex.api.dto

import no.nav.helse.flex.sykmelding.application.*
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.utils.objectMapper
import java.time.LocalDate

data class SendSykmeldingRequestDTO(
    val erOpplysningeneRiktige: YesOrNo,
    val arbeidssituasjon: Arbeidssituasjon,
    val arbeidsgiverOrgnummer: String? = null,
    val harEgenmeldingsdager: YesOrNo? = null,
    val riktigNarmesteLeder: YesOrNo? = null,
    val arbeidsledig: Arbeidsledig? = null,
    val egenmeldingsdager: List<LocalDate>? = null,
    val egenmeldingsperioder: List<EgenmeldingsperiodeDTO>? = null,
    val fisker: Fisker? = null,
    val harBruktEgenmelding: YesOrNo? = null,
    val harForsikring: YesOrNo? = null,
    val uriktigeOpplysninger: List<UriktigeOpplysning>? = null,
) {
    fun tilArbeidssituasjonBrukerInfo(): ArbeidssituasjonBrukerInfo =
        when (arbeidssituasjon) {
            Arbeidssituasjon.ARBEIDSLEDIG -> {
                ArbeidsledigBrukerInfo(arbeidsledigFraOrgnummer = arbeidsledig?.arbeidsledigFraOrgnummer)
            }
            Arbeidssituasjon.ARBEIDSTAKER -> ArbeidstakerBrukerInfo(arbeidsgiverOrgnummer = arbeidsgiverOrgnummer)
            Arbeidssituasjon.PERMITTERT -> PermittertBrukerInfo(arbeidsledigFraOrgnummer = arbeidsledig?.arbeidsledigFraOrgnummer)
            Arbeidssituasjon.FISKER -> {
                requireNotNull(fisker) { "Fisker må være satt for fisker" }
                FiskerBrukerInfo(
                    lottOgHyre = enumValueOf(fisker.lottOgHyre.name),
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                )
            }
            Arbeidssituasjon.FRILANSER -> FrilanserBrukerInfo
            Arbeidssituasjon.NAERINGSDRIVENDE -> NaringsdrivendeBrukerInfo
            Arbeidssituasjon.JORDBRUKER -> JordbrukerBrukerInfo
            Arbeidssituasjon.ANNET -> AnnetArbeidssituasjonBrukerInfo
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

    private fun konverterJaNeiSvar(svar: YesOrNo?): List<Svar> =
        when (svar) {
            YesOrNo.YES -> listOf(Svar(verdi = "JA"))
            YesOrNo.NO -> listOf(Svar(verdi = "NEI"))
            else -> emptyList()
        }
}

enum class YesOrNo {
    YES,
    NO,
}

data class EgenmeldingsperiodeDTO(
    val fom: LocalDate?,
    val tom: LocalDate?,
)

data class Arbeidsledig(
    val arbeidsledigFraOrgnummer: String? = null,
)

data class Fisker(
    val blad: Blad,
    val lottOgHyre: LottOgHyre,
)

enum class UriktigeOpplysning {
    ANDRE_OPPLYSNINGER,
    ARBEIDSGIVER,
    DIAGNOSE,
    PERIODE,
    SYKMELDINGSGRAD_FOR_HOY,
    SYKMELDINGSGRAD_FOR_LAV,
}
