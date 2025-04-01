package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.application.BrukerSvar
import java.time.Instant

data class SykmeldingHendelse(
    internal val databaseId: String? = null,
    val status: HendelseStatus,
    val arbeidstakerInfo: ArbeidstakerInfo? = null,
    val brukerSvar: BrukerSvar? = null,
    val tilleggsinfo: Tilleggsinfo? = null,
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
