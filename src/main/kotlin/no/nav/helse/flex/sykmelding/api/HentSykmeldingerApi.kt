package no.nav.helse.flex.sykmelding.api

import SykmeldingDtoKonverterer
import no.nav.helse.flex.logger
import no.nav.helse.flex.sykmelding.api.dto.SykmeldingDTO
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import no.nav.helse.flex.sykmelding.logikk.SykmeldingHenter
import no.nav.helse.flex.tokenx.TOKENX
import no.nav.helse.flex.tokenx.TokenxValidering
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

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingUuid}/brukerinformasjon")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun getBrukerinformasjon(
        @PathVariable("sykmeldingUuid") sykmeldingUuid: String,
    ): ResponseEntity<Any> {
        TODO("Ikke implementert")
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
}
