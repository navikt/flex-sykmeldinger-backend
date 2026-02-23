package no.nav.helse.flex.smregmapping

import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import java.time.OffsetDateTime

data class MottattSykmeldingSmregDto(
    val sykmelding: SykmeldingSmregDto,
    val kafkaMetadata: KafkaMetadataSmregDto,
)

data class BekreftetSendtSykmeldingSmregDto(
    val sykmelding: SykmeldingSmregDto,
    val kafkaMetadata: KafkaMetadataSmregDto,
    val event: SykmeldingStatusKafkaDTO,
)

data class KafkaMetadataSmregDto(
    val sykmeldingId: String,
    val timestamp: OffsetDateTime,
    val fnr: String,
    val source: String,
)
