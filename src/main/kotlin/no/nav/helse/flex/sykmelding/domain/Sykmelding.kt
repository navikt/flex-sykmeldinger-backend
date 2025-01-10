package no.nav.helse.flex.sykmelding.domain

import java.time.Instant

data class Sykmelding(
    val sykmeldingGrunnlag: ISykmeldingGrunnlag,
    val statuser: List<SykmeldingStatus> = emptyList(),
) {
    val sykmeldingId: String
        get() = sykmeldingGrunnlag.id

    fun sisteStatus(): SykmeldingStatus {
        return statuser.sortedBy { it.timestamp }.last()
    }
}

data class SykmeldingStatus(
    val status: String,
    val sporsmal: String?,
    val timestamp: Instant,
)
