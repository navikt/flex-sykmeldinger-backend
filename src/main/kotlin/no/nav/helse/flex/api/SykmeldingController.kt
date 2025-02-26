package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.BrukerinformasjonDTO
import no.nav.helse.flex.api.dto.NarmesteLederDTO
import no.nav.helse.flex.api.dto.SykmeldingDTO
import no.nav.helse.flex.api.dto.VirksomhetDTO
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.config.IdentService
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.config.TOKENX
import no.nav.helse.flex.config.TokenxValidering
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.sykmelding.domain.*
import no.nav.helse.flex.sykmelding.logikk.SykmeldingHenter
import no.nav.helse.flex.utils.logger
import no.nav.helse.flex.virksomhet.VirksomhetHenterService
import no.nav.helse.flex.virksomhet.domain.Virksomhet
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.util.function.Supplier

@Controller
class SykmeldingController(
    private val sykmeldingHenter: SykmeldingHenter,
    private val tokenxValidering: TokenxValidering,
    private val identService: IdentService,
    private val sykmeldingRepository: ISykmeldingRepository,
    private val virksomhetHenterService: VirksomhetHenterService,
    private val nowFactory: Supplier<Instant>,
    private val sykmeldingDtoKonverterer: SykmeldingDtoKonverterer,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
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

        val sykmeldinger = sykmeldingRepository.findAllByPersonIdenter(identer)
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
        val fnr = tokenxValidering.validerFraDittSykefravaerOgHentFnr()

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
        val identer = tokenxValidering.hentIdenter()

        if (sykmeldingId == "null") {
            logger.warn("Mottok kall for å hente sykmelding med id null, sender 404 Not Found")
            return ResponseEntity.notFound().build()
        }
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            logger.warn("Fant ikke sykmeldingen")
            return ResponseEntity.notFound().build()
        }
        if (sykmelding.sykmeldingGrunnlag.pasient.fnr !in identer.alle()) {
            logger.warn("Person har ikke tilgang til sykmelding")
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
        val fnr = tokenxValidering.validerFraDittSykefravaerOgHentFnr()

        val sykmlding =
            sykmeldingRepository.findBySykmeldingId(sykmeldingId)
                ?: return ResponseEntity.notFound().build()
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
        val fnr = tokenxValidering.validerFraDittSykefravaerOgHentFnr()
        val sykmelding = sykmeldingRepository.findBySykmeldingId(sykmeldingId)
        if (sykmelding == null) {
            logger.warn("Fant ikke sykmeldingen")
            return ResponseEntity.notFound().build()
        }
        if (sykmelding.pasientFnr != fnr) {
            logger.warn("Fnr på sykmeldingen er forskjellig fra token")
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val arbeidstakerInfo: ArbeidstakerInfo? =
            if (sendBody.arbeidsgiverOrgnummer != null) {
                val arbeidsforhold = arbeidsforholdRepository.getAllByFnr(fnr)
                val valgtArbeidsforhold = arbeidsforhold.find { it.orgnummer == sendBody.arbeidsgiverOrgnummer }
                if (valgtArbeidsforhold == null) {
                    throw IllegalArgumentException("Fant ikke arbeidsgiver med orgnummer ${sendBody.arbeidsgiverOrgnummer}")
                }
                ArbeidstakerInfo(
                    arbeidsgiver =
                        Arbeidsgiver(
                            orgnummer = valgtArbeidsforhold.orgnummer,
                            juridiskOrgnummer = valgtArbeidsforhold.juridiskOrgnummer,
                            orgnavn = valgtArbeidsforhold.orgnavn,
                        ),
                )
            } else {
                null
            }

        val besvartSykmelding =
            sykmelding.leggTilStatus(
                SykmeldingHendelse(
                    // TODO: Finn ut forskjell på SENDT og BEKREFTET
                    status = HendelseStatus.SENDT,
                    opprettet = nowFactory.get(),
                    sporsmalSvar = sendBody.tilSporsmalListe(),
                    arbeidstakerInfo = arbeidstakerInfo,
                ),
            )

        val lagretSykmelding = sykmeldingRepository.save(besvartSykmelding)
        val konvertertSykmelding = sykmeldingDtoKonverterer.konverterSykmelding(lagretSykmelding)

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
