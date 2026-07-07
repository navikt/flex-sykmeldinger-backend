package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.gateways.KafkaMetadataDTO
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage
import no.nav.helse.flex.sykmeldinghendelse.SYKMELDINGSTATUS_LEESAH_SOURCE
import no.nav.helse.flex.sykmeldinghendelse.UtdatertFormatException
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/*
Denne prøver å opprette en tilnærmet lik sykmelding kafka melding som sykepengesoknad-backend ville mottatt på sykmelding sendt/bekreftet topics
Er til bruk for å kunne hente sykmeldinger synkront for opt-in og ventetidssøknader, siden de ikke lagres i sykepengesoknad-backend enda
*/
@Component
class SykmeldingKafkaMessageKonverterer(
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
) {
    val log = logger()

    fun opprettTilsvarendeSykmeldingKafkaMessage(sykmelding: Sykmelding): SykmeldingKafkaMessage? {
        val sykmeldingDTO =
            SykmeldingDtoRegler
                .skjermForPasientDersomSpesifisert(sykmeldingDtoKonverterer.konverter(sykmelding))
                .let { dto ->
                    dto.copy(
                        merknader =
                            if (dto.behandlingsutfall.erUnderBehandling || dto.behandlingsutfall.status == RegelStatusDTO.INVALID) {
                                listOfNotNull(dto.merknader?.first())
                            } else {
                                null
                            },
                    )
                }
        val timestampIkkeRelevant = OffsetDateTime.MIN
        val kafkaMetadata =
            KafkaMetadataDTO(
                sykmeldingId = sykmelding.sykmeldingId,
                timestamp = timestampIkkeRelevant,
                fnr = sykmelding.pasientFnr,
                source = SYKMELDINGSTATUS_LEESAH_SOURCE,
            )
        val event =
            try {
                SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                    sykmeldingHendelse = sykmelding.sisteHendelse(),
                    sykmeldingId = sykmelding.sykmeldingId,
                )
            } catch (e: UtdatertFormatException) {
                log.warn("Hopper over: ${e.message}", e)
                return null
            }
        return SykmeldingKafkaMessage(
            kafkaMetadata = kafkaMetadata,
            event = event,
            sykmelding = sykmeldingDTO,
        )
    }
}
