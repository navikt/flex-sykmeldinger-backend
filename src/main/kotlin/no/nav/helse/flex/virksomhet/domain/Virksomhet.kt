package no.nav.helse.flex.virksomhet.domain

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder

data class Virksomhet(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val navn: String,
    val aktivtArbeidsforhold: Boolean,
    val naermesteLeder: NarmesteLeder?,
)
