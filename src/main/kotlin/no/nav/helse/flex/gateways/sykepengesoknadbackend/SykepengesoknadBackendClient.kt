package no.nav.helse.flex.gateways.sykepengesoknadbackend

import no.nav.helse.flex.api.dto.ArbeidssituasjonDTO
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage

interface SykepengesoknadBackendClient {
    fun harSoknad(
        sykmeldingId: String,
        arbeidssituasjon: ArbeidssituasjonDTO,
    ): Boolean

    fun opprettOptIn(sykmeldingKafkaMessage: SykmeldingKafkaMessage)
}
