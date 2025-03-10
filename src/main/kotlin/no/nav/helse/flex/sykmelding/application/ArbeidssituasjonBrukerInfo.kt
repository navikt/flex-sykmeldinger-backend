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
    val lottOgHyre: FiskerLottOgHyre,
    val arbeidsgiverOrgnummer: String? = null,
) : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.FISKER

    init {
        if (lottOgHyre in setOf(FiskerLottOgHyre.HYRE, FiskerLottOgHyre.BEGGE)) {
            require(arbeidsgiverOrgnummer != null) { "ArbeidsgiverOrgnummer må være satt for HYRE" }
        }
    }
}

data object FrilanserBrukerInfo : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.FRILANSER
}

data object JordbrukerBrukerInfo : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.JORDBRUKER
}

data object NaringsdrivendeBrukerInfo : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.NAERINGSDRIVENDE
}

data object AnnetArbeidssituasjonBrukerInfo : ArbeidssituasjonBrukerInfo {
    override val arbeidssituasjon = Arbeidssituasjon.ANNET
}

enum class FiskerLottOgHyre {
    LOTT,
    HYRE,
    BEGGE,
}
