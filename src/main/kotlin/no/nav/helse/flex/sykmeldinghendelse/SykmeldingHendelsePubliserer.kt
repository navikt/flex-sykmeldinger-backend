package no.nav.helse.flex.sykmeldinghendelse

import no.nav.helse.flex.gateways.KafkaMetadataDTO
import no.nav.helse.flex.gateways.SykmeldingStatusKafkaMessageDTO
import no.nav.helse.flex.gateways.SykmeldingStatusKafkaProducer
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer
import no.nav.helse.flex.tsmsykmeldingstatus.dto.SykmeldingStatusKafkaDTO
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

const val SYKMELDINGSTATUS_LEESAH_SOURCE = "flex-sykmeldinger-backend"

@Component
class SykmeldingHendelsePubliserer(
    private val sykmeldingStatusKafkaProducer: SykmeldingStatusKafkaProducer,
) {
    fun publiserSisteHendelse(sykmelding: Sykmelding) {
        publiserHendelse(
            fnr = sykmelding.pasientFnr,
            sykmeldingId = sykmelding.sykmeldingId,
            sykmeldingHendelse = sykmelding.sisteHendelse(),
        )
    }

    fun publiserHendelse(
        fnr: String,
        sykmeldingId: String,
        sykmeldingHendelse: SykmeldingHendelse,
    ) {
        val status =
            sammenstillSykmeldingStatusKafkaMessageDTO(
                fnr = fnr,
                sykmeldingStatusKafkaDTO =
                    SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                        sykmeldingHendelse = sykmeldingHendelse,
                        sykmeldingId = sykmeldingId,
                    ),
            )
        sykmeldingStatusKafkaProducer.produserSykmeldingStatus(status)
    }

    companion object {
        internal fun sammenstillSykmeldingStatusKafkaMessageDTO(
            fnr: String,
            sykmeldingStatusKafkaDTO: SykmeldingStatusKafkaDTO,
        ): SykmeldingStatusKafkaMessageDTO {
            val sykmeldingId = sykmeldingStatusKafkaDTO.sykmeldingId
            val metadataDTO =
                KafkaMetadataDTO(
                    sykmeldingId = sykmeldingId,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                    fnr = fnr,
                    source = SYKMELDINGSTATUS_LEESAH_SOURCE,
                )

            return SykmeldingStatusKafkaMessageDTO(
                kafkaMetadata = metadataDTO,
                event = sykmeldingStatusKafkaDTO,
            )
        }
    }
}
