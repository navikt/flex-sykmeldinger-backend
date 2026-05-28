package no.nav.helse.flex.gateways.sykepengesoknadbackend

import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage

interface SykepengesoknadBackendClient {
    fun harSoknad(sykmeldingId: String): Boolean

    fun opprettOptIn(sykmeldingKafkaMessage: SykmeldingKafkaMessage)
}
