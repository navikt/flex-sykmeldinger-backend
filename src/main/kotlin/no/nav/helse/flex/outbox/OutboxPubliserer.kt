package no.nav.helse.flex.outbox

interface OutboxPubliserer {
    fun outboxSykmeldingHendelse(
        fnr: String,
        sykmeldingId: String,
        sykmeldingHendelseId: String,
    )

    fun sendUsendteForEldsteLedigeFnr()
}
