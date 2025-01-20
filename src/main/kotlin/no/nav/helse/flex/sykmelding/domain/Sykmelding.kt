package no.nav.helse.flex.sykmelding.domain

import org.postgresql.util.PGobject
import java.time.Instant
import kotlin.collections.plus

data class Sykmelding(
    internal val databaseId: String? = null,
    val sykmeldingGrunnlag: ISykmeldingGrunnlag,
    val statuser: List<SykmeldingStatus>,
    val opprettet: Instant,
    val oppdatert: Instant,
) {
    init {
        require(statuser.isNotEmpty()) { "Må ha en status" }
    }

    val sykmeldingId: String
        get() = sykmeldingGrunnlag.id

    fun sisteStatus(): SykmeldingStatus =
        statuser.maxByOrNull { it.opprettet } ?: error("Fant ikke status for sykmeldingen. Skal ikke skje.")

    fun leggTilStatus(sykmeldingStatus: SykmeldingStatus): Sykmelding =
        this.copy(
            statuser = this.statuser + sykmeldingStatus,
            oppdatert = sykmeldingStatus.opprettet,
        )
}

data class SykmeldingStatus(
    internal val databaseId: String? = null,
    val status: String,
    val opprettet: Instant,
    // TODO: Change type
    val sporsmalSvar: PGobject? = null,
)
