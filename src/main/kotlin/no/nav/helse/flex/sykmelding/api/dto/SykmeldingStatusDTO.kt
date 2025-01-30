package no.nav.helse.flex.sykmelding.api.dto

import java.time.OffsetDateTime

data class SykmeldingStatusDTO(
    val statusEvent: String,
    val timestamp: OffsetDateTime,
    val arbeidsgiver: ArbeidsgiverStatusDTO?,
    val sporsmalOgSvarListe: List<SporsmalDTO>,
    // TODO: This is nullable because older sykmeldinger are not migrated to the new format
    val brukerSvar: SykmeldingFormResponse?,
)

data class ArbeidsgiverStatusDTO(
    val orgnummer: String,
    val juridiskOrgnummer: String?,
    val orgNavn: String,
)
