package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.arbeidsgiverdetaljer.ArbeidsgiverDetaljerService
import no.nav.helse.flex.arbeidsgiverdetaljer.domain.ArbeidsgiverDetaljer
import no.nav.helse.flex.clients.syketilfelle.ErUtenforVentetidResponse
import no.nav.helse.flex.clients.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.config.IdentService
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.config.TOKENX
import no.nav.helse.flex.config.TokenxValidering
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.sykmelding.application.SykmeldingHandterer
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.tidligereArbeidsgivere.TidligereArbeidsgivereHandterer
import no.nav.helse.flex.utils.logger
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
class SykmeldingController(
    private val tokenxValidering: TokenxValidering,
    private val identService: IdentService,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val arbeidsgiverDetaljerService: ArbeidsgiverDetaljerService,
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
    private val sykmeldingHandterer: SykmeldingHandterer,
    private val syketilfelleClient: SyketilfelleClient,
    private val sykmeldingRegelAvklaringer: SykmeldingRegelAvklaringer,
) {
    private val logger = logger()

    @GetMapping("/api/v1/sykmeldinger")
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getSykmeldinger(): ResponseEntity<List<SykmeldingDTO>> {
        val identer = tokenxValidering.hentIdenter()

        val sykmeldinger = sykmeldingHandterer.hentAlleSykmeldinger(identer)
        val konverterteSykmeldinger = sykmeldinger.map { sykmeldingDtoKonverterer.konverterSykmelding(it) }
        return ResponseEntity.ok(konverterteSykmeldinger)
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingId}/tidligere-arbeidsgivere")
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getTidligereArbeidsgivere(
        @PathVariable("sykmeldingId") sykmeldingId: String,
    ): ResponseEntity<List<TidligereArbeidsgiver>> {
        val identer = tokenxValidering.hentIdenter()

        val sykmeldinger = sykmeldingHandterer.hentAlleSykmeldinger(identer)
        val tidligereArbeidsgivere = TidligereArbeidsgivereHandterer.finnTidligereArbeidsgivere(sykmeldinger, sykmeldingId)

        return ResponseEntity.ok(tidligereArbeidsgivere)
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingId}")
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getSykmelding(
        @PathVariable("sykmeldingId") sykmeldingId: String,
    ): ResponseEntity<SykmeldingDTO> {
        val identer = tokenxValidering.hentIdenter()

        if (sykmeldingId == "null") {
            logger.warn("Mottok kall for å hente sykmelding med id null, sender 404 Not Found")
            return ResponseEntity.notFound().build()
        }

        val sykmelding = sykmeldingHandterer.hentSykmelding(sykmeldingId = sykmeldingId, identer = identer)

        val konvertertSykmelding = sykmeldingDtoKonverterer.konverterSykmelding(sykmelding)
        return ResponseEntity.ok(konvertertSykmelding)
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingId}/brukerinformasjon")
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getBrukerinformasjon(
        @PathVariable("sykmeldingId") sykmeldingId: String,
    ): ResponseEntity<BrukerinformasjonDTO> {
        val identer = tokenxValidering.hentIdenter()

        val sykmelding =
            sykmeldingRepository.findBySykmeldingId(sykmeldingId)
                ?: return ResponseEntity.notFound().build()
        if (sykmelding.pasientFnr !in identer.alle()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val sykmeldingPeriode = sykmelding.fom to sykmelding.tom
        val arbeidsgiverDetaljer = arbeidsgiverDetaljerService.hentArbeidsgiverDetaljerForPerson(identer, sykmeldingPeriode)

        return ResponseEntity.ok(
            BrukerinformasjonDTO(
                arbeidsgivere = arbeidsgiverDetaljer.map { it.konverterTilDto() },
                erOverSyttiAar =
                    sykmeldingRegelAvklaringer.erOverSyttiAar(
                        pasientFnr = sykmelding.pasientFnr,
                        fom = sykmelding.fom,
                    ),
            ),
        )
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingId}/er-utenfor-ventetid")
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getErUtenforVentetid(
        @PathVariable sykmeldingId: String,
    ): ResponseEntity<ErUtenforVentetidResponse> {
        val identer = tokenxValidering.hentIdenter()

        val sykmelding = sykmeldingHandterer.hentSykmelding(sykmeldingId = sykmeldingId, identer = identer)

        val erUtenforVentetid =
            syketilfelleClient.getErUtenforVentetid(
                sykmeldingId = sykmelding.sykmeldingId,
                identer = identer,
            )

        return ResponseEntity.ok(erUtenforVentetid)
    }

    @PostMapping("/api/v1/sykmeldinger/{sykmeldingId}/send")
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun sendSykmelding(
        @PathVariable("sykmeldingId") sykmeldingId: String,
        @RequestBody sendSykmeldingRequestDTO: SendSykmeldingRequestDTO,
    ): ResponseEntity<SykmeldingDTO> {
        val identer = tokenxValidering.hentIdenter()

        val brukerSvar = sendSykmeldingRequestDTO.tilBrukerSvar()

        val sykmelding =
            sykmeldingHandterer.sendSykmelding(
                sykmeldingId = sykmeldingId,
                identer = identer,
                brukerSvar = brukerSvar,
            )
        logger.info("Sender sykmelding ${sykmelding.sykmeldingId} med status ${sykmelding.sisteHendelse().status}")

        val konvertertSykmelding = sykmeldingDtoKonverterer.konverterSykmelding(sykmelding)
        return ResponseEntity.ok(konvertertSykmelding)
    }

    @PostMapping("/api/v1/sykmeldinger/{sykmeldingUuid}/change-status")
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun changeSykmeldingStatus(
        @PathVariable("sykmeldingUuid") sykmeldingUuid: String,
        @RequestBody changeStatus: SykmeldingChangeStatus,
    ): ResponseEntity<SykmeldingDTO> {
        val identer = tokenxValidering.hentIdenter()

        val sykmelding =
            when (changeStatus) {
                SykmeldingChangeStatus.AVBRYT -> {
                    sykmeldingHandterer.avbrytSykmelding(sykmeldingUuid, identer)
                }
                SykmeldingChangeStatus.BEKREFT_AVVIST -> {
                    sykmeldingHandterer.bekreftAvvistSykmelding(sykmeldingUuid, identer)
                }
            }

        val konvertertSykmelding = sykmeldingDtoKonverterer.konverterSykmelding(sykmelding)
        return ResponseEntity.ok(konvertertSykmelding)
    }

    private fun TokenxValidering.hentIdenter(): PersonIdenter =
        identService.hentFolkeregisterIdenterMedHistorikkForFnr(this.validerFraDittSykefravaerOgHentFnr())
}

internal fun ArbeidsgiverDetaljer.konverterTilDto(): ArbeidsgiverDetaljerDTO =
    ArbeidsgiverDetaljerDTO(
        orgnummer = this.orgnummer,
        juridiskOrgnummer = this.juridiskOrgnummer,
        navn = this.navn,
        aktivtArbeidsforhold = this.aktivtArbeidsforhold,
        naermesteLeder = this.naermesteLeder?.konverterTilDto(),
    )

internal fun NarmesteLeder.konverterTilDto(): NarmesteLederDTO {
    requireNotNull(this.narmesteLederNavn) { "Narmeste leder navn må være satt" }
    return NarmesteLederDTO(
        navn = this.narmesteLederNavn,
        orgnummer = this.orgnummer,
    )
}

enum class SykmeldingChangeStatus {
    AVBRYT,
    BEKREFT_AVVIST,
}
