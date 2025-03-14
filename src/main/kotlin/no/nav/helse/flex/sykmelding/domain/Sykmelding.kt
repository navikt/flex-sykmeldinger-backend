package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.domain.tsm.*
import java.time.Instant
import java.time.LocalDate
import kotlin.collections.plus

data class Sykmelding(
    internal val databaseId: String? = null,
    val sykmeldingGrunnlag: ISykmeldingGrunnlag,
    val meldingsinformasjon: Meldingsinformasjon,
    val validation: ValidationResult,
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

    val erAvvist: Boolean
        get() = validation.status == RuleType.INVALID

    val erEgenmeldt: Boolean
        get() = meldingsinformasjon.type == MetadataType.EGENMELDT

    fun sisteStatus(): SykmeldingHendelse = statuser.lastMaxBy { it.opprettet } ?: error("Ingen statuser. Skal ikke skje.")

    fun leggTilStatus(sykmeldingHendelse: SykmeldingHendelse): Sykmelding =
        this.copy(
            statuser = this.statuser + sykmeldingHendelse,
            oppdatert = sykmeldingHendelse.opprettet,
        )
}

data class SykmeldingHendelse(
    internal val databaseId: String? = null,
    val status: HendelseStatus,
    val sporsmalSvar: List<Sporsmal>? = null,
    val arbeidstakerInfo: ArbeidstakerInfo? = null,
    val opprettet: Instant,
)

enum class HendelseStatus {
    APEN,
    AVBRUTT,
    SENDT_TIL_NAV,
    SENDT_TIL_ARBEIDSGIVER,
    BEKREFTET_AVVIST,
    UTGATT,
}

private fun <T, R : Comparable<R>> Iterable<T>.lastMaxBy(selector: (T) -> R): T? = this.sortedBy(selector).lastOrNull()
