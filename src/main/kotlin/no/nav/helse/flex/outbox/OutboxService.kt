package no.nav.helse.flex.outbox

import no.nav.helse.flex.gateways.SykmeldingNotifikasjon

interface OutboxService {
    fun outboxSykmeldingHendelse(
        fnr: String,
        sykmeldingId: String,
        sykmeldingHendelseId: String,
    )

    fun outboxSykmeldingBrukernotifikasjon(sykmeldingNotifikasjon: SykmeldingNotifikasjon)

    fun sendUsendteForEldsteLedigeFnr()
}
