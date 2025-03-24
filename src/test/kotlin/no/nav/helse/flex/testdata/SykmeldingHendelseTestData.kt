package no.nav.helse.flex.testdata

import no.nav.helse.flex.sykmelding.application.BrukerSvar
import no.nav.helse.flex.sykmelding.domain.*
import java.time.Instant

fun lagSykmeldingHendelse(
    status: HendelseStatus = HendelseStatus.APEN,
    sporsmalSvar: List<Sporsmal>? = null,
    arbeidstakerInfo: ArbeidstakerInfo? = null,
    tilleggsinfo: Tilleggsinfo? = null,
    brukerSvar: BrukerSvar? = null,
    opprettet: Instant = Instant.parse("2021-01-01T00:00:00.00Z"),
) = SykmeldingHendelse(
    status = status,
    sporsmalSvar = sporsmalSvar,
    tilleggsinfo = tilleggsinfo,
    brukerSvar = brukerSvar,
    opprettet = opprettet,
    arbeidstakerInfo = arbeidstakerInfo,
)

fun lagArbeidstakerInfo(arbeidsgiver: Arbeidsgiver = lagArbeidsgiver()): ArbeidstakerInfo =
    ArbeidstakerInfo(
        arbeidsgiver = arbeidsgiver,
    )
