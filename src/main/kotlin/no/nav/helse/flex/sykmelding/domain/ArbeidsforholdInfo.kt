@file:Suppress("ktlint:standard:filename")

package no.nav.helse.flex.sykmelding.domain

data class ArbeidstakerInfo(
    val arbeidsgiver: Arbeidsgiver,
)
//
// data class Arbeidsgiver(
//    val orgnummer: String,
//    val juridiskOrgnummer: String,
//    val orgnavn: String,
//    val erAktivtArbeidsforhold: Boolean,
//    val narmesteLeder: NarmesteLeder?,
// )
//
// data class NarmesteLeder(
//    val navn: String,
// )
