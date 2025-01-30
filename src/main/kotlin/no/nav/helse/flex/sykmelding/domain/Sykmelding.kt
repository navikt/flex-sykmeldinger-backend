package no.nav.helse.flex.sykmelding.domain

import org.postgresql.util.PGobject
import java.time.Instant
import java.time.LocalDate
import kotlin.collections.plus

data class Sykmelding(
    internal val databaseId: String? = null,
    val sykmeldingGrunnlag: ISykmeldingGrunnlag,
    val statuser: List<SykmeldingHendelse>,
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

    fun sisteStatus(): SykmeldingHendelse =
        statuser.maxByOrNull { it.opprettet } ?: error("Fant ikke status for sykmeldingen. Skal ikke skje.")

    fun leggTilStatus(sykmeldingHendelse: SykmeldingHendelse): Sykmelding =
        this.copy(
            statuser = this.statuser + sykmeldingHendelse,
            oppdatert = sykmeldingHendelse.opprettet,
        )
}

data class SykmeldingHendelse(
    internal val databaseId: String? = null,
    val status: HendelseStatus,
    val sporsmalSvar: PGobject? = null,
    val opprettet: Instant,
)

enum class HendelseStatus {
    APEN,
    AVBRUTT,
    BEKREFTET,
    SENDT,
    UTGATT,
}
