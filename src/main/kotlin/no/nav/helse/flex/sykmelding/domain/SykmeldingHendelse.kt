package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import java.time.Instant

data class SykmeldingHendelse(
    internal val databaseId: String? = null,
    val status: HendelseStatus,
    val sporsmalSvar: List<Sporsmal>? = null,
    val arbeidstakerInfo: ArbeidstakerInfo? = null,
    val tidligereArbeidsgiver: TidligereArbeidsgiver? = null,
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
