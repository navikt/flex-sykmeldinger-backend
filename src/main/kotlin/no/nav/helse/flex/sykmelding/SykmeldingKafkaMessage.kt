package no.nav.helse.flex.sykmelding

import no.nav.helse.flex.api.dto.SykmeldingDTO
import no.nav.helse.flex.gateways.KafkaMetadataDTO
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO

data class SykmeldingKafkaMessage(
    val sykmelding: SykmeldingDTO,
    val kafkaMetadata: KafkaMetadataDTO,
    val event: SykmeldingStatusKafkaDTO,
)
