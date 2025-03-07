package no.nav.helse.flex.api.dto

import no.nav.helse.flex.sykmelding.domain.*
import java.time.LocalDate

data class SendSykmeldingRequestDTO(
    val erOpplysningeneRiktige: YesOrNo,
    val arbeidssituasjon: Arbeidssituasjon,
    val arbeidsgiverOrgnummer: String?,
    val harEgenmeldingsdager: YesOrNo?,
    val riktigNarmesteLeder: YesOrNo?,
    val arbeidsledig: Arbeidsledig?,
    val egenmeldingsdager: List<LocalDate>?,
    val egenmeldingsperioder: List<EgenmeldingsperiodeDTO>?,
    val fisker: Fisker?,
    val harBruktEgenmelding: YesOrNo?,
    val harForsikring: YesOrNo?,
    val uriktigeOpplysninger: List<UriktigeOpplysning>?,
) {
    fun tilArbeidssituasjonBrukerInfo(): ArbeidssituasjonBrukerInfo =
        when (arbeidssituasjon) {
            Arbeidssituasjon.ARBEIDSLEDIG -> {
                ArbeidsledigBrukerInfo(arbeidsledigFraOrgnummer = arbeidsledig?.arbeidsledigFraOrgnummer)
            }
            Arbeidssituasjon.ARBEIDSTAKER -> ArbeidstakerBrukerInfo(arbeidsgiverOrgnummer = arbeidsgiverOrgnummer)
            Arbeidssituasjon.FRILANSER -> FrilanserBrukerInfo()
            Arbeidssituasjon.NAERINGSDRIVENDE -> NaringsdrivendeBrukerInfo()
            Arbeidssituasjon.FISKER -> {
                requireNotNull(fisker) { "Fisker må være satt for fisker" }
                FiskerBrukerInfo(
                    blad = enumValueOf(fisker.blad.name),
                    lottOgHyre = enumValueOf(fisker.lottOgHyre.name),
                    arbeidsgiverOrgnummer = arbeidsgiverOrgnummer,
                )
            }
            Arbeidssituasjon.JORDBRUKER -> JordbrukerBrukerInfo(arbeidsgiverOrgnummer = arbeidsgiverOrgnummer)
            Arbeidssituasjon.PERMITTERT -> PermittertBrukerInfo(arbeidsledigFraOrgnummer = arbeidsledig?.arbeidsledigFraOrgnummer)
            Arbeidssituasjon.ANNET -> AnnetArbeidssituasjonBrukerInfo()
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
                    svartype = Svartype.FRITEKST,
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
        harEgenmeldingsdager?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
                    svartype = Svartype.JA_NEI,
                    svar = konverterJaNeiSvar(it),
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
                    svartype = Svartype.FRITEKST,
                    svar = listOf(Svar(verdi = it)),
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

enum class Arbeidssituasjon {
    ARBEIDSTAKER,
    FRILANSER,
    NAERINGSDRIVENDE,
    FISKER,
    JORDBRUKER,
    ARBEIDSLEDIG,
    PERMITTERT,
    ANNET,
}
