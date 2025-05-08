package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.application.BrukerSvar
import no.nav.helse.flex.sykmelding.domain.*
import java.time.Instant

fun lagSykmeldingHendelse(
    status: HendelseStatus = HendelseStatus.APEN,
    tilleggsinfo: Tilleggsinfo? = null,
    brukerSvar: BrukerSvar? = null,
    source: String? = null,
    hendelseOpprettet: Instant = Instant.parse("2021-01-01T00:00:00.00Z"),
    lokaltOpprettet: Instant = Instant.parse("2021-01-01T00:00:00.00Z"),
) = SykmeldingHendelse(
    status = status,
    tilleggsinfo = tilleggsinfo,
    brukerSvar = brukerSvar,
    hendelseOpprettet = hendelseOpprettet,
    lokaltOpprettet = lokaltOpprettet,
    source = source,
)
