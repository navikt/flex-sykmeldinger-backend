package no.nav.helse.flex.api

import jakarta.servlet.http.HttpServletRequest
import no.nav.helse.flex.api.dto.SykmeldingDTO
import no.nav.helse.flex.config.IdentService
import no.nav.helse.flex.config.Roles
import no.nav.helse.flex.config.TokenValideringService
import no.nav.helse.flex.config.getToken
import no.nav.helse.flex.domain.AuditEntry
import no.nav.helse.flex.domain.EventType
import no.nav.helse.flex.gateways.AuditLogProducer
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
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

@RestController
class SykmeldingTexasController(
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
    private val sykmeldingLeser: SykmeldingLeser,
    private val tokenValideringService: TokenValideringService,
    private val identService: IdentService,
    private val auditLogProducer: AuditLogProducer,
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
                        // tilpasning for hva vi får fra gammel kafka kø
                        .let { dto ->
                            dto.copy(
                                merknader =
                                    if (dto.behandlingsutfall.erUnderBehandling) {
                                        listOfNotNull(dto.merknader?.first())
                                    } else {
                                        null
                                    },
                            )
                        }
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

    @PostMapping(value = ["/api/v1/flex/sykmeldinger"])
    @ResponseBody
    fun hentSykmeldingerForFlexInternal(
        @RequestBody fnrRequest: FnrRequest,
        request: HttpServletRequest,
    ): ResponseEntity<List<SykmeldingDTO>> {
        val navIdent =
            tokenValideringService.validerGruppeOgHentNavIdent(
                token = request.getToken(),
                identityProvider = "entra_id",
            )

        val identer = identService.hentFolkeregisterIdenterMedHistorikkForFnr(fnrRequest.fnr)
        val sykmeldinger = sykmeldingLeser.hentAlleSykmeldinger(identer)

        auditLogProducer.lagAuditLog(
            AuditEntry(
                appNavn = "flex-sykmeldinger-backend",
                utførtAv = navIdent,
                oppslagPå = fnrRequest.fnr,
                eventType = EventType.READ,
                forespørselTillatt = true,
                oppslagUtførtTid = Instant.now(),
                beskrivelse = "Henter alle sykmeldinger",
                requestUrl = URI.create(request.requestURL.toString()),
                requestMethod = "POST",
            ),
        )

        return ResponseEntity.ok(sykmeldinger.map { sykmeldingDtoKonverterer.konverter(it) })
    }
}

data class SykmeldingKafkaMessageResponse(
    val sykmeldinger: List<SykmeldingKafkaMessage>,
)

data class SykmeldingerKafkaMessageRequest(
    val sykmeldingIder: List<String>,
    val fom: LocalDate? = null,
)

data class FnrRequest(
    val fnr: String,
)
