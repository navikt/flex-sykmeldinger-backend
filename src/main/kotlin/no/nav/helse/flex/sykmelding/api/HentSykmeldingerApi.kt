package no.nav.helse.flex.sykmelding.api

import SykmeldingDtoKonverterer
import no.nav.helse.flex.logger
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.sykmelding.api.dto.BrukerinformasjonDTO
import no.nav.helse.flex.sykmelding.api.dto.NarmesteLederDTO
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingDTO
import no.nav.helse.flex.sykmelding.api.dto.VirksomhetDTO
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.logikk.SykmeldingHenter
import no.nav.helse.flex.tokenx.TOKENX
import no.nav.helse.flex.tokenx.TokenxValidering
import no.nav.helse.flex.virksomhet.VirksomhetHenterService
import no.nav.helse.flex.virksomhet.domain.Virksomhet
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class HentSykmeldingerApi(
    private val sykmeldingHenter: SykmeldingHenter,
    private val tokenxValidering: TokenxValidering,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val virksomhetHenterService: VirksomhetHenterService,
) {
    private val sykmeldingDtoKonverterer = SykmeldingDtoKonverterer()
    private val logger = logger()

    @GetMapping("/api/v1/sykmeldinger")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getSykmeldinger(): ResponseEntity<List<SykmeldingDTO>> {
        val fnr = tokenxValidering.validerFraDittSykefravaer()

        val sykmeldinger = sykmeldingRepository.findAllByFnr(fnr)
        val konverterteSykmeldinger = sykmeldinger.map { sykmeldingDtoKonverterer.konverterSykmelding(it) }
        return ResponseEntity.ok(konverterteSykmeldinger)
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingUuid}/tidligere-arbeidsgivere")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getTidligereArbeidsgivere(
        @PathVariable("sykmeldingUuid") sykmeldingUuid: String,
    ): ResponseEntity<Any> {
        val fnr = tokenxValidering.validerFraDittSykefravaer()

        val tidligereArbeidsgivere = sykmeldingHenter.finnTidligereArbeidsgivere(fnr, sykmeldingUuid)
        return ResponseEntity.ok(tidligereArbeidsgivere)
    }

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
        val fnr = tokenxValidering.validerFraDittSykefravaer()

        if (sykmeldingId == "null") {
            logger.warn("Mottok kall for å hente sykmelding med id null, sender 404 Not Found")
            return ResponseEntity.notFound().build()
        }
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            logger.warn("Fant ikke sykmeldingen")
            return ResponseEntity.notFound().build()
        }
        if (sykmelding.sykmeldingGrunnlag.pasient.fnr != fnr) {
            logger.warn("Fnr på sykmeldingen er forskjellig fra token")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
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
        val fnr = tokenxValidering.validerFraDittSykefravaer()
        val sykmlding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmlding == null) {
            return ResponseEntity.notFound().build()
        }
        if (sykmlding.pasientFnr != fnr) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val sykmeldingPeriode = sykmlding.fom to sykmlding.tom
        val virksomheter = virksomhetHenterService.hentVirksomheterForPersonInnenforPeriode(fnr, sykmeldingPeriode)

        return ResponseEntity.ok(
            BrukerinformasjonDTO(
                arbeidsgivere = virksomheter.map { it.konverterTilDto() },
            ),
        )
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingUuid}/er-utenfor-ventetid")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getErUtenforVentetid(
        @PathVariable("sykmeldingUuid") sykmeldingUuid: String,
    ): ResponseEntity<Any> {
        TODO("Ikke implementert")
    }

    @PostMapping("/api/v1/sykmeldinger/{sykmeldingUuid}/send")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun sendSykmelding(
        @PathVariable("sykmeldingUuid") sykmeldingUuid: String,
        @RequestBody sendSykmeldingValues: Any,
    ): ResponseEntity<Any> {
        TODO("Ikke implementert")
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
        @RequestBody changeStatus: Any,
    ): ResponseEntity<Any> {
        TODO("Ikke implementert")
    }

    private fun Virksomhet.konverterTilDto(): VirksomhetDTO =
        VirksomhetDTO(
            orgnummer = this.orgnummer,
            juridiskOrgnummer = this.juridiskOrgnummer,
            navn = this.navn,
            aktivtArbeidsforhold = this.aktivtArbeidsforhold,
            naermesteLeder = this.naermesteLeder?.konverterTilDto(),
        )

    private fun NarmesteLeder.konverterTilDto(): NarmesteLederDTO? =
        if (this.narmesteLederNavn == null) {
            null
        } else {
            NarmesteLederDTO(
                navn = this.narmesteLederNavn,
                orgnummer = this.orgnummer,
            )
        }
}
