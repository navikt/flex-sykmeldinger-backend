package no.nav.helse.flex.sykmelding.domain

data class ArbeidstakerInfo(
    val arbeidsgiver: Arbeidsgiver,
)

data class Arbeidsgiver(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val orgnavn: String,
)
