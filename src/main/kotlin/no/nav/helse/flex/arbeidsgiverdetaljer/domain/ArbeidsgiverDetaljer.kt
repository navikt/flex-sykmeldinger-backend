package no.nav.helse.flex.arbeidsgiverdetaljer.domain

import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import java.time.LocalDate

data class ArbeidsgiverDetaljer(
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val navn: String,
    val fom: LocalDate,
    val tom: LocalDate?,
    val aktivtArbeidsforhold: Boolean,
    val naermesteLeder: NarmesteLeder?,
)
