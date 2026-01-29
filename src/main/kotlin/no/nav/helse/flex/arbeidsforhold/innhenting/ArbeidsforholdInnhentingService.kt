package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.config.IdentService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

@Service
class ArbeidsforholdInnhentingService(
    private val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val identService: IdentService,
    private val nowFactory: Supplier<Instant> = Supplier { Instant.now() },
) {
    @Transactional(rollbackFor = [Exception::class])
    fun synkroniserArbeidsforholdForPersonFuture(fnr: String): CompletableFuture<ArbeidsforholdSynkronisering> =
        CompletableFuture.completedFuture(synkroniserArbeidsforholdForPerson(fnr))

    @Transactional(rollbackFor = [Exception::class])
    fun synkroniserArbeidsforholdForPerson(fnr: String): ArbeidsforholdSynkronisering {
        val identerOgEksterneArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson(fnr)
        val alleIdenter =
            run {
                val identerFraAareg = identerOgEksterneArbeidsforhold.identer
                val identerFraPdl = identService.hentFolkeregisterIdenterMedHistorikkForFnr(fnr)
                (identerFraAareg.alle() + identerFraPdl.alle()).distinct()
            }
        val interneArbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(alleIdenter)
        val synkroniserteArbeidsforhold =
            synkroniserArbeidsforhold(
                interneArbeidsforhold = interneArbeidsforhold,
                eksterneArbeidsforhold = identerOgEksterneArbeidsforhold.eksterneArbeidsforhold,
                fnr = fnr,
                now = nowFactory.get(),
            )
        lagreSynkroniserteArbeidsforhold(synkroniserteArbeidsforhold)
        return synkroniserteArbeidsforhold
    }

    fun slettArbeidsforhold(navArbeidsforholdId: String) {
        arbeidsforholdRepository.deleteByNavArbeidsforholdId(navArbeidsforholdId)
    }

    internal fun lagreSynkroniserteArbeidsforhold(arbeidsforholdSynkronisering: ArbeidsforholdSynkronisering) {
        if (arbeidsforholdSynkronisering.opprett.isNotEmpty()) {
            arbeidsforholdRepository.saveAll(arbeidsforholdSynkronisering.opprett)
        }
        if (arbeidsforholdSynkronisering.oppdater.isNotEmpty()) {
            arbeidsforholdRepository.saveAll(arbeidsforholdSynkronisering.oppdater)
        }
        if (arbeidsforholdSynkronisering.slett.isNotEmpty()) {
            arbeidsforholdRepository.deleteAll(arbeidsforholdSynkronisering.slett)
        }
    }

    companion object {
        internal fun synkroniserArbeidsforhold(
            interneArbeidsforhold: List<Arbeidsforhold>,
            eksterneArbeidsforhold: List<EksterntArbeidsforhold>,
            fnr: String,
            now: Instant = Instant.now(),
        ): ArbeidsforholdSynkronisering {
            val eksterneArbeidsforholdVedId = eksterneArbeidsforhold.associateBy { it.navArbeidsforholdId }
            val interneArbeidsforholdVedId = interneArbeidsforhold.associateBy { it.navArbeidsforholdId }

            val opprettArbeidsforholdId = eksterneArbeidsforholdVedId.keys - interneArbeidsforholdVedId.keys
            val oppdaterArbeidsforholdId = interneArbeidsforholdVedId.keys.intersect(eksterneArbeidsforholdVedId.keys)
            val slettArbeidsforholdId = interneArbeidsforholdVedId.keys - eksterneArbeidsforholdVedId.keys

            val opprettArbeidsforhold =
                opprettArbeidsforholdId.map { navArbeidsforholdId ->
                    val eksterntArbeidsforhold = eksterneArbeidsforholdVedId[navArbeidsforholdId]!!
                    Arbeidsforhold(
                        navArbeidsforholdId = eksterntArbeidsforhold.navArbeidsforholdId,
                        fnr = fnr,
                        orgnummer = eksterntArbeidsforhold.orgnummer,
                        juridiskOrgnummer = eksterntArbeidsforhold.juridiskOrgnummer,
                        orgnavn = eksterntArbeidsforhold.orgnavn,
                        fom = eksterntArbeidsforhold.fom,
                        tom = eksterntArbeidsforhold.tom,
                        arbeidsforholdType = eksterntArbeidsforhold.arbeidsforholdType,
                        opprettet = now,
                    )
                }

            val oppdaterteArbeidsforhold =
                oppdaterArbeidsforholdId.map { navArbeidsforholdId ->
                    val interntArbeidsforhold = interneArbeidsforholdVedId[navArbeidsforholdId]!!
                    val eksterntArbeidsforhold = eksterneArbeidsforholdVedId[navArbeidsforholdId]!!
                    interntArbeidsforhold.copy(
                        navArbeidsforholdId = eksterntArbeidsforhold.navArbeidsforholdId,
                        orgnummer = eksterntArbeidsforhold.orgnummer,
                        juridiskOrgnummer = eksterntArbeidsforhold.juridiskOrgnummer,
                        orgnavn = eksterntArbeidsforhold.orgnavn,
                        fom = eksterntArbeidsforhold.fom,
                        tom = eksterntArbeidsforhold.tom,
                        arbeidsforholdType = eksterntArbeidsforhold.arbeidsforholdType,
                        oppdatert = now,
                    )
                }

            val slettedeArbeidsforhold =
                slettArbeidsforholdId.map { navArbeidsforholdId ->
                    interneArbeidsforholdVedId[navArbeidsforholdId]!!
                }

            val opprettNyligeArbeidsforhold = opprettArbeidsforhold.filter { harVaertAnsattSiste4Mnd(it.tom, now = now) }

            return ArbeidsforholdSynkronisering(
                opprett = opprettNyligeArbeidsforhold,
                oppdater = oppdaterteArbeidsforhold,
                slett = slettedeArbeidsforhold,
            )
        }

        internal fun harVaertAnsattSiste4Mnd(
            sluttDato: LocalDate?,
            now: Instant,
        ): Boolean {
            val fireMndSiden = LocalDate.ofInstant(now, UTC).minusMonths(4)
            return sluttDato == null || sluttDato.isAfter(fireMndSiden)
        }
    }
}

data class ArbeidsforholdSynkronisering(
    val opprett: List<Arbeidsforhold> = emptyList(),
    val oppdater: List<Arbeidsforhold> = emptyList(),
    val slett: List<Arbeidsforhold> = emptyList(),
) {
    companion object {
        val INGEN = ArbeidsforholdSynkronisering()
    }

    fun erIngen(): Boolean = this == INGEN

    operator fun plus(other: ArbeidsforholdSynkronisering): ArbeidsforholdSynkronisering =
        ArbeidsforholdSynkronisering(
            opprett = this.opprett + other.opprett,
            oppdater = this.oppdater + other.oppdater,
            slett = this.slett + other.slett,
        )

    fun toLogString(): String =
        if (erIngen()) {
            "ingen endringer"
        } else {
            val opprettIds = opprett.map { it.navArbeidsforholdId }
            val oppdaterIds = oppdater.map { it.navArbeidsforholdId }
            val slettIds = slett.map { it.navArbeidsforholdId }
            val sizeProps =
                mapOf(
                    "opprett" to opprettIds.size,
                    "oppdater" to oppdaterIds.size,
                    "slett" to slettIds.size,
                ).filterValues { it > 0 }
            val navArbeidsforholdIdProps =
                mapOf(
                    "opprettNavArbeidsforholdId" to opprettIds,
                    "oppdaterNavArbeidsforholdId" to oppdaterIds,
                    "slettNavArbeidsforholdId" to slettIds,
                ).filterValues { it.isNotEmpty() }
            (sizeProps + navArbeidsforholdIdProps).toString()
        }
}
