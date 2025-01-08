package no.nav.helse.flex.sykmelding.api

import no.nav.helse.flex.logger
import no.nav.helse.flex.sykmelding.logikk.SykmeldingHenter
import no.nav.helse.flex.tokenx.TOKENX
import no.nav.helse.flex.tokenx.TokenxValidering
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class HentSykmeldingerApi(
    private val sykmeldingHenter: SykmeldingHenter,
    private val tokenxValidering: TokenxValidering,
) {
    private val logger = logger()

    @GetMapping("/api/v1/sykmeldinger")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        claimMap = ["acr=idporten-loa-high"],
    )
    fun getSykmeldinger(): ResponseEntity<Any> {
        val fnr = tokenxValidering.validerFraDittSykefravaer()
        val sykmeldinger = sykmeldingHenter.getSykmeldinger(fnr = fnr)
        return ResponseEntity.ok(sykmeldinger)
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingUuid}/tidligere-arbeidsgivere")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        claimMap = ["acr=idporten-loa-high"],
    )
    fun getTidligereArbeidsgivere(
        @PathVariable("sykmeldingUuid") sykmeldingUuid: String,
    ): ResponseEntity<Any> {
        val fnr = tokenxValidering.validerFraDittSykefravaer()

        val tidligereArbeidsgivere = sykmeldingHenter.finnTidligereArbeidsgivere(fnr, sykmeldingUuid)
        return ResponseEntity.ok(tidligereArbeidsgivere)
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingUuid}")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        claimMap = ["acr=idporten-loa-high"],
    )
    fun getSykmelding(
        @PathVariable("sykmeldingUuid") sykmeldingUuid: String,
    ): ResponseEntity<Any> {
        val fnr = tokenxValidering.validerFraDittSykefravaer()

        if (sykmeldingUuid == "null") {
            logger.warn("Mottok kall for å hente sykmelding med id null, sender 404 Not Found")
            return ResponseEntity.notFound().build()
        } else {
            val sykmelding = sykmeldingHenter.getSykmelding(fnr, sykmeldingUuid)

            return if (sykmelding == null) {
                ResponseEntity.notFound().build()
            } else {
                ResponseEntity.ok(sykmelding)
            }
        }
    }

    @GetMapping("/api/v1/sykmeldinger/{sykmeldingUuid}/brukerinformasjon")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        claimMap = ["acr=idporten-loa-high"],
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
        claimMap = ["acr=idporten-loa-high"],
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
        claimMap = ["acr=idporten-loa-high"],
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
        claimMap = ["acr=idporten-loa-high"],
    )
    fun changeSykmeldingStatus(
        @PathVariable("sykmeldingUuid") sykmeldingUuid: String,
        @RequestBody changeStatus: Any,
    ): ResponseEntity<Any> {
        TODO("Ikke implementert")
    }

}
