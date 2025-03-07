package no.nav.helse.flex.sykmelding.domain

data class ArbeidsforholdInfo(
    val arbeidstaker: ArbeidstakerInfo? = null,
)

data class ArbeidstakerInfo(
    val arbeidsgiver: Arbeidsgiver,
)

data class Arbeidsgiver(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val orgnavn: String,
)
