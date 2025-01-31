package no.nav.helse.flex.sykmelding.api.dto

import java.time.OffsetDateTime

data class SykmeldingStatusDTO(
    val statusEvent: String,
    val timestamp: OffsetDateTime,
    val arbeidsgiver: ArbeidsgiverStatusDTO?,
    // TODO: Remove this, use "brukerSvar" istedet
    val sporsmalOgSvarListe: List<SporsmalDTO>,
    // TODO: make this not nullable
    val brukerSvar: SykmeldingSporsmalSvarDto?,
)

data class ArbeidsgiverStatusDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String?,
    val orgNavn: String,
)
