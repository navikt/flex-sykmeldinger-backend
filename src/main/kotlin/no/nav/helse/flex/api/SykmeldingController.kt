package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.BrukerinformasjonDTO
import no.nav.helse.flex.api.dto.NarmesteLederDTO
import no.nav.helse.flex.api.dto.SykmeldingDTO
import no.nav.helse.flex.api.dto.VirksomhetDTO
import no.nav.helse.flex.config.IdentService
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.config.TOKENX
import no.nav.helse.flex.config.TokenxValidering
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.sykmelding.application.SykmeldingHandterer
import no.nav.helse.flex.sykmelding.domain.*
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

    @PostMapping("/api/v1/sykmeldinger/{sykmeldingId}/send")
    @ResponseBody
    @ProtectedWithClaims(
        issuer = TOKENX,
        combineWithOr = true,
        claimMap = ["acr=Level4", "acr=idporten-loa-high"],
    )
    fun sendSykmelding(
        @PathVariable("sykmeldingId") sykmeldingId: String,
        @RequestBody sendBody: SendBody,
    ): ResponseEntity<SykmeldingDTO> {
        val identer = tokenxValidering.hentIdenter()

        val sykmelding =
            sykmeldingHandterer.sendSykmelding(
                sykmeldingId = sykmeldingId,
                identer = identer,
                arbeidsgiverOrgnummer = sendBody.arbeidsgiverOrgnummer,
                sporsmalSvar = sendBody.tilSporsmalListe(),
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
        @RequestBody changeStatus: Any,
    ): ResponseEntity<Any> {
        TODO("Ikke implementert")
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

data class SendBody(
    val erOpplysningeneRiktige: String,
    val arbeidsgiverOrgnummer: String?,
    val arbeidssituasjon: String,
    val harEgenmeldingsdager: String?,
    val riktigNarmesteLeder: String?,
) {
    fun tilSporsmalListe(): List<Sporsmal> {
        val sporsmal = mutableListOf<Sporsmal>()
        sporsmal.add(
            Sporsmal(
                tag = SporsmalTag.ER_OPPLYSNINGENE_RIKTIGE,
                svartype = Svartype.JA_NEI,
                svar = listOf(Svar(verdi = konverterJaNeiSvar(erOpplysningeneRiktige))),
            ),
        )
        arbeidsgiverOrgnummer?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.ARBEIDSGIVER_ORGNUMMER,
                    svartype = Svartype.FRITEKST,
                    svar = listOf(Svar(verdi = it)),
                ),
            )
        }
        sporsmal.add(
            Sporsmal(
                tag = SporsmalTag.ARBEIDSSITUASJON,
                svartype = Svartype.RADIO,
                svar = listOf(Svar(verdi = arbeidssituasjon)),
            ),
        )
        harEgenmeldingsdager?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.HAR_BRUKT_EGENMELDING,
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = konverterJaNeiSvar(it))),
                ),
            )
        }
        riktigNarmesteLeder?.let {
            sporsmal.add(
                Sporsmal(
                    tag = SporsmalTag.RIKTIG_NARMESTE_LEDER,
                    svartype = Svartype.JA_NEI,
                    svar = listOf(Svar(verdi = konverterJaNeiSvar(it))),
                ),
            )
        }
        return sporsmal
    }

    private fun konverterJaNeiSvar(svar: String): String =
        when (svar) {
            "YES" -> "JA"
            "NO" -> "NEI"
            else -> svar
        }
}
