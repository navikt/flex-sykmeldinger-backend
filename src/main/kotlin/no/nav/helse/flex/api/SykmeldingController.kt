package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.*
import no.nav.helse.flex.clients.syketilfelle.ErUtenforVentetidResponse
import no.nav.helse.flex.clients.syketilfelle.SyketilfelleClient
import no.nav.helse.flex.config.IdentService
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.config.TOKENX
import no.nav.helse.flex.config.TokenxValidering
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.sykmelding.application.SykmeldingHandterer
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.virksomhet.VirksomhetHenterService
import no.nav.helse.flex.virksomhet.domain.Virksomhet
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class SykmeldingController(
    private val tokenxValidering: TokenxValidering,
    private val identService: IdentService,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val virksomhetHenterService: VirksomhetHenterService,
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
    private val sykmeldingHandterer: SykmeldingHandterer,
    private val syketilfelleClient: SyketilfelleClient,
) {
    private val logger = logger()

    @GetMapping("/api/v1/sykmeldinger")
    @ResponseBody
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
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getTidligereArbeidsgivere(
        @PathVariable("sykmeldingId") sykmeldingId: String,
    ): ResponseEntity<Any> = ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build()

    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    @GetMapping("/api/v1/sykmeldinger/{sykmeldingId}")
    @ResponseBody
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
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getBrukerinformasjon(
        @PathVariable("sykmeldingId") sykmeldingId: String,
    ): ResponseEntity<BrukerinformasjonDTO> {
        val identer = tokenxValidering.hentIdenter()

        val sykmlding =
            sykmeldingRepository.findBySykmeldingId(sykmeldingId)
                ?: return ResponseEntity.notFound().build()
        if (sykmlding.pasientFnr !in identer.alle()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val sykmeldingPeriode = sykmlding.fom to sykmlding.tom
        val virksomheter = virksomhetHenterService.hentVirksomheterForPersonInnenforPeriode(identer, sykmeldingPeriode)

        return ResponseEntity.ok(
            BrukerinformasjonDTO(
                arbeidsgivere = virksomheter.map { it.konverterTilDto() },
            ),
        )
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingId}/er-utenfor-ventetid")
    @ResponseBody
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
    @ResponseBody
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

        val arbeidssituasjonBrukerInfo = sendSykmeldingRequestDTO.tilArbeidssituasjonBrukerInfo()
        val sporsmalSvar = sendSykmeldingRequestDTO.tilSporsmalListe()

        val sykmelding =
            sykmeldingHandterer.sendSykmelding(
                sykmeldingId = sykmeldingId,
                identer = identer,
                arbeidssituasjonBrukerInfo = arbeidssituasjonBrukerInfo,
                sporsmalSvar = sporsmalSvar,
            )

        val konvertertSykmelding = sykmeldingDtoKonverterer.konverterSykmelding(sykmelding)

        return ResponseEntity.ok(konvertertSykmelding)
    }

    @PostMapping("/api/v1/sykmeldinger/{sykmeldingUuid}/change-status")
    @ResponseBody
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

internal fun Virksomhet.konverterTilDto(): VirksomhetDTO =
    VirksomhetDTO(
        orgnummer = this.orgnummer,
        juridiskOrgnummer = this.juridiskOrgnummer,
        navn = this.navn,
        aktivtArbeidsforhold = this.aktivtArbeidsforhold,
        naermesteLeder = this.naermesteLeder?.konverterTilDto(),
    )

internal fun NarmesteLeder.konverterTilDto(): NarmesteLederDTO? =
    if (this.narmesteLederNavn == null) {
        null
    } else {
        NarmesteLederDTO(
            navn = this.narmesteLederNavn,
            orgnummer = this.orgnummer,
        )
    }

enum class SykmeldingChangeStatus {
    AVBRYT,
    BEKREFT_AVVIST,
}
