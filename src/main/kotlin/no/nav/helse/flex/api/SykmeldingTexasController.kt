package no.nav.helse.flex.api

import jakarta.servlet.http.HttpServletRequest
import no.nav.helse.flex.gateways.KafkaMetadataDTO
import no.nav.helse.flex.sykmelding.SykmeldingKafkaMessage
import no.nav.helse.flex.sykmelding.SykmeldingLeser
import no.nav.helse.flex.sykmeldinghendelse.SYKMELDINGSTATUS_LEESAH_SOURCE
import no.nav.helse.flex.tsmsykmeldingstatus.SykmeldingHendelseTilKafkaKonverterer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
class SykmeldingTexasController(
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
    private val sykmeldingLeser: SykmeldingLeser,
    @param:Value("\${SYKEPENGESOKNAD_BACKEND_CLIENT_ID}")
    private val sykepengesoknadBackendClientId: String,
    private val tokenValideringService: TokenValideringService,
) {
    data class SykmeldingKafkaMessageResponse(
        val sykmeldinger: List<SykmeldingKafkaMessage>,
    )

    data class SykmeldingerRequest(
        val sykmeldingIder: List<String>,
    )

    @PostMapping(value = ["/api/v1/sykmeldinger/kafka"])
    @ResponseBody
    fun hentSykmeldingKafkaMessage(
        @RequestBody sykmeldingerRequest: SykmeldingerRequest,
        request: HttpServletRequest,
    ): ResponseEntity<SykmeldingKafkaMessageResponse> {
        val token = request.getToken()
        if (!tokenValideringService.validerToken(token, "entra_id")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        if (!tokenValideringService.validerClientIdFraToken(token!!, listOf(Roles.ROLE_SYKEPENGESOKNAD_BACKEND))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val sykmeldinger =
            sykmeldingLeser.hentAlleSykmeldingerFraIder(sykmeldingIder = sykmeldingerRequest.sykmeldingIder)

        val sykmeldingDtoer =
            sykmeldinger.map {
                val sykmeldingDTO =
                    SykmeldingDtoRegler.skjermForPasientDersomSpesifisert(sykmeldingDtoKonverterer.konverter(it))
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

    private fun HttpServletRequest.getToken(): String? = this.getHeader("Authorization")?.removePrefix("Bearer ")
}
