package no.nav.helse.flex.sykmelding.domain

import org.postgresql.util.PGobject
import java.time.Instant
import kotlin.collections.plus

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

    fun leggTilStatus(sykmeldingStatus: SykmeldingStatus): Sykmelding =
        this.copy(
            statuser = this.statuser + sykmeldingStatus,
        )
}

data class SykmeldingStatus(
    internal val databaseId: String? = null,
    val status: String,
    val sporsmalSvar: PGobject? = null,
    val timestamp: Instant = Instant.now(),
)
