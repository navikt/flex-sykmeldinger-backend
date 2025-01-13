package no.nav.helse.flex.sykmelding.domain

fun lagStatusNy(): SykmeldingStatus {
    return SykmeldingStatus(
        status = "NY",
    )
}
