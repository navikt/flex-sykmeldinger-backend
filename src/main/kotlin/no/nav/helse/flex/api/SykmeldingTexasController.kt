package no.nav.helse.flex.api

import jakarta.servlet.http.HttpServletRequest
import no.nav.helse.flex.config.Roles
import no.nav.helse.flex.config.TokenValideringService
import no.nav.helse.flex.config.getToken
import no.nav.helse.flex.gateways.KafkaMetadataDTO
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage
import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmeldinghendelse.SYKMELDINGSTATUS_LEESAH_SOURCE
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
class SykmeldingTexasController(
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
    private val sykmeldingLeser: SykmeldingLeser,
    private val tokenValideringService: TokenValideringService,
) {
    @PostMapping(value = ["/api/v1/sykmeldinger/kafka"])
    @ResponseBody
    fun hentSykmeldingerKafkaMessage(
        @RequestBody sykmeldingerKafkaMessageRequest: SykmeldingerKafkaMessageRequest,
        request: HttpServletRequest,
    ): ResponseEntity<SykmeldingKafkaMessageResponse> {
        request
            .getToken()
            .let {
                tokenValideringService.validerTokenOgRolle(
                    token = it,
                    identityProvider = "entra_id",
                    forventedeRoller = listOf(Roles.ROLE_SYKEPENGESOKNAD_BACKEND),
                )
            }

        val sykmeldinger =
            sykmeldingLeser.hentAlleSykmeldingerFraIderFom(
                sykmeldingIder = sykmeldingerKafkaMessageRequest.sykmeldingIder,
                fom = sykmeldingerKafkaMessageRequest.fom,
            )

        val sykmeldingDtoer =
            sykmeldinger.map {
                val sykmeldingDTO =
                    SykmeldingDtoRegler
                        .skjermForPasientDersomSpesifisert(sykmeldingDtoKonverterer.konverter(it))
                        // tilpasning for hva vi får fra syfosmregister
                        .let { dto -> dto.copy(merknader = dto.merknader?.ifEmpty { null }) }
                val timestampIkkeRelevant = OffsetDateTime.MIN
                val kafkaMetadata =
                    KafkaMetadataDTO(
                        sykmeldingId = it.sykmeldingId,
                        timestamp = timestampIkkeRelevant,
                        fnr = it.pasientFnr,
                        source = SYKMELDINGSTATUS_LEESAH_SOURCE,
                    )
                val event =
                    SykmeldingHendelseTilKafkaKonverterer.konverterSykmeldingHendelseTilKafkaDTO(
                        sykmeldingHendelse = it.sisteHendelse(),
                        sykmeldingId = it.sykmeldingId,
                    )
                SykmeldingKafkaMessage(
                    kafkaMetadata = kafkaMetadata,
                    event = event,
                    sykmelding = sykmeldingDTO,
                )
            }
        return ResponseEntity.ok(SykmeldingKafkaMessageResponse(sykmeldingDtoer))
    }
}

data class SykmeldingKafkaMessageResponse(
    val sykmeldinger: List<SykmeldingKafkaMessage>,
)

data class SykmeldingerKafkaMessageRequest(
    val sykmeldingIder: List<String>,
    val fom: LocalDate? = null,
)
