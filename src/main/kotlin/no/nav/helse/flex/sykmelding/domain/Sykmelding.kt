package no.nav.helse.flex.sykmelding.domain

import java.time.Instant

data class Sykmelding(
    internal val databaseId: String? = null,
    val sykmeldingGrunnlag: ISykmeldingGrunnlag,
    val statuser: List<SykmeldingStatus>,
) {
    init {
        require(statuser.isNotEmpty()) { "Må ha en status" }
    }

    val sykmeldingId: String
        get() = sykmeldingGrunnlag.id

    fun sisteStatus(): SykmeldingStatus {
        return statuser.sortedBy { it.timestamp }.last()
    }
}

data class SykmeldingStatus(
    internal val databaseId: String? = null,
    val status: String,
    val sporsmal: String? = null,
    val timestamp: Instant = Instant.now(),
)
