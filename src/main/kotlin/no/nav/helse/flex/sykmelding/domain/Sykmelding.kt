package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.domain.tsm.*
import java.time.Instant
import java.time.LocalDate
import kotlin.collections.plus

data class Sykmelding(
    internal val databaseId: String? = null,
    val sykmeldingGrunnlag: ISykmeldingGrunnlag,
    val validation: ValidationResult,
    val hendelser: List<SykmeldingHendelse>,
    val opprettet: Instant,
    val hendelseOppdatert: Instant,
    val sykmeldingGrunnlagOppdatert: Instant,
    val validationOppdatert: Instant,
) {
    init {
        require(hendelser.isNotEmpty()) { "Må ha minst én hendelse" }
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

    fun sisteHendelse(): SykmeldingHendelse = hendelser.lastMaxBy { it.opprettet } ?: error("Ingen hendelser. Skal ikke skje.")

    fun leggTilHendelse(sykmeldingHendelse: SykmeldingHendelse): Sykmelding =
        this.copy(
            hendelser = this.hendelser + sykmeldingHendelse,
            sykmeldingGrunnlagOppdatert = sykmeldingHendelse.opprettet,
        )
}

private fun <T, R : Comparable<R>> Iterable<T>.lastMaxBy(selector: (T) -> R): T? = this.sortedBy(selector).lastOrNull()
