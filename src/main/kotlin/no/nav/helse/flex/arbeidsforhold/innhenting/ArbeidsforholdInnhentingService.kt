package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.config.IdentService
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.function.Supplier

@Service
class ArbeidsforholdInnhentingService(
    private val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val identService: IdentService,
    private val nowFactory: Supplier<Instant> = Supplier { Instant.now() },
) {
    private val log = logger()

    @Transactional(rollbackFor = [Exception::class])
    fun synkroniserArbeidsforholdForPerson(fnr: String): SynkroniserteArbeidsforhold {
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
        log.info(synkroniserteArbeidsforhold.toLogString())
        return synkroniserteArbeidsforhold
    }

    fun slettArbeidsforhold(navArbeidsforholdId: String) {
        arbeidsforholdRepository.deleteByNavArbeidsforholdId(navArbeidsforholdId)
    }

    internal fun lagreSynkroniserteArbeidsforhold(synkroniserteArbeidsforhold: SynkroniserteArbeidsforhold) {
        if (synkroniserteArbeidsforhold.skalOpprettes.isNotEmpty()) {
            arbeidsforholdRepository.saveAll(synkroniserteArbeidsforhold.skalOpprettes)
        }
        if (synkroniserteArbeidsforhold.skalOppdateres.isNotEmpty()) {
            arbeidsforholdRepository.saveAll(synkroniserteArbeidsforhold.skalOppdateres)
        }
        if (synkroniserteArbeidsforhold.skalSlettes.isNotEmpty()) {
            arbeidsforholdRepository.deleteAll(synkroniserteArbeidsforhold.skalSlettes)
        }
    }

    companion object {
        internal fun synkroniserArbeidsforhold(
            interneArbeidsforhold: List<Arbeidsforhold>,
            eksterneArbeidsforhold: List<EksterntArbeidsforhold>,
            fnr: String,
            now: Instant = Instant.now(),
        ): SynkroniserteArbeidsforhold {
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
                    )
                }

            val slettedeArbeidsforhold =
                slettArbeidsforholdId.map { navArbeidsforholdId ->
                    interneArbeidsforholdVedId[navArbeidsforholdId]!!
                }

            val opprettNyligeArbeidsforhold = opprettArbeidsforhold.filter { harVaertAnsattSiste4Mnd(it.tom, now = now) }

            return SynkroniserteArbeidsforhold(
                skalOpprettes = opprettNyligeArbeidsforhold,
                skalOppdateres = oppdaterteArbeidsforhold,
                skalSlettes = slettedeArbeidsforhold,
            )
        }

        private fun harVaertAnsattSiste4Mnd(
            sluttDato: LocalDate?,
            now: Instant,
        ): Boolean {
            val fireMndSiden = LocalDate.ofInstant(now, UTC).minusMonths(4)
            return sluttDato == null || sluttDato.isAfter(fireMndSiden)
        }
    }
}

data class SynkroniserteArbeidsforhold(
    val skalOpprettes: List<Arbeidsforhold> = emptyList(),
    val skalOppdateres: List<Arbeidsforhold> = emptyList(),
    val skalSlettes: List<Arbeidsforhold> = emptyList(),
) {
    fun toLogString(): String {
        val opprettetIds = skalOpprettes.map { it.navArbeidsforholdId }
        val oppdatertIds = skalOppdateres.map { it.navArbeidsforholdId }
        val slettetIds = skalSlettes.map { it.navArbeidsforholdId }
        return "SynkroniserteArbeidsforhold(" +
            "antallOpprettet=${opprettetIds.count()}, " +
            "antallOppdatert=${oppdatertIds.count()}, " +
            "antallSlettet=${slettetIds.count()}, " +
            "opprettet navArbeidsforholdId: $opprettetIds, " +
            "oppdatert navArbeidsforholdId: $oppdatertIds, " +
            "slettet navArbeidsforholdId: $slettetIds" +
            ")"
    }
}
