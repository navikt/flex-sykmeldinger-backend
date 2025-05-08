package no.nav.helse.flex.sykmelding.domain

import no.nav.helse.flex.sykmelding.application.BrukerSvar
import java.time.Instant

data class SykmeldingHendelse(
    internal val databaseId: String? = null,
    val status: HendelseStatus,
    val brukerSvar: BrukerSvar? = null,
    val tilleggsinfo: Tilleggsinfo? = null,
    val source: String? = null,
    val hendelseOpprettet: Instant,
    val lokaltOpprettet: Instant,
) {
    companion object {
        const val LOKAL_SOURCE = "flex-sykmeldinger-backend"
    }
}

enum class HendelseStatus {
    APEN,
    AVBRUTT,
    SENDT_TIL_NAV,
    SENDT_TIL_ARBEIDSGIVER,
    BEKREFTET_AVVIST,
    UTGATT,
}
