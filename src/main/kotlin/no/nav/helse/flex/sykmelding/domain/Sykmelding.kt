package no.nav.helse.flex.sykmelding.domain

import org.postgresql.util.PGobject
import java.time.Instant
import java.time.LocalDate
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
        require(sykmeldingGrunnlag.aktivitet.isNotEmpty()) { "SykmeldingGrunnlag må ha minst én aktivitet" }
    }

    val sykmeldingId: String
        get() = sykmeldingGrunnlag.id

    val pasientFnr: String
        get() = sykmeldingGrunnlag.pasient.fnr

    val fom: LocalDate
        get() = sykmeldingGrunnlag.aktivitet.minOf { it.fom }

    val tom: LocalDate
        get() = sykmeldingGrunnlag.aktivitet.maxOf { it.tom }

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
    val status: StatusEvent,
    val opprettet: Instant,
    val sporsmalSvar: PGobject? = null,
)

enum class StatusEvent {
    APEN,
    AVBRUTT,
    BEKREFTET,
    SENDT,
    UTGATT,
}
