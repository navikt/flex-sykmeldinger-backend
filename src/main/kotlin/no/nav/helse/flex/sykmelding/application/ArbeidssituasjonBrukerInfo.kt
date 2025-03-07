package no.nav.helse.flex.sykmelding.application

import no.nav.helse.flex.api.dto.Arbeidssituasjon

sealed interface ArbeidssituasjonBrukerInfo {
    val arbeidssituasjon: Arbeidssituasjon
}

data class ArbeidstakerBrukerInfo(
    val arbeidsgiverOrgnummer: String? = null,
) : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.ARBEIDSTAKER
}

data class ArbeidsledigBrukerInfo(
    val arbeidsledigFraOrgnummer: String? = null,
) : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.ARBEIDSLEDIG
}

data class PermittertBrukerInfo(
    val arbeidsledigFraOrgnummer: String? = null,
) : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.PERMITTERT
}

data class FiskerBrukerInfo(
    val blad: FiskerBlad,
    val lottOgHyre: FiskerLottOgHyre,
    val arbeidsgiverOrgnummer: String? = null,
) : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.FISKER

    init {
        if (lottOgHyre in setOf(FiskerLottOgHyre.HYRE, FiskerLottOgHyre.BEGGE)) {
            require(arbeidsgiverOrgnummer != null) { "ArbeidsgiverOrgnummer må være satt for LOTT" }
        }
    }
}

class FrilanserBrukerInfo : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.FRILANSER

    override fun equals(other: Any?) = other is FrilanserBrukerInfo

    override fun hashCode() = FrilanserBrukerInfo::class.hashCode()
}

data class JordbrukerBrukerInfo(
    val arbeidsgiverOrgnummer: String? = null,
) : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.JORDBRUKER

    override fun equals(other: Any?) = other is JordbrukerBrukerInfo

    override fun hashCode() = JordbrukerBrukerInfo::class.hashCode()
}

class NaringsdrivendeBrukerInfo : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.NAERINGSDRIVENDE

    override fun equals(other: Any?) = other is NaringsdrivendeBrukerInfo

    override fun hashCode() = NaringsdrivendeBrukerInfo::class.hashCode()
}

data class AnnetArbeidssituasjonBrukerInfo(
    val arbeidsgiverOrgnummer: String? = null,
) : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.ANNET

    override fun equals(other: Any?) = other is AnnetArbeidssituasjonBrukerInfo

    override fun hashCode() = AnnetArbeidssituasjonBrukerInfo::class.hashCode()
}

enum class FiskerBlad {
    A,
    B,
}

enum class FiskerLottOgHyre {
    LOTT,
    HYRE,
    BEGGE,
}
