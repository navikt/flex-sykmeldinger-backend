package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC
import java.util.function.Supplier

data class SynkroniserteArbeidsforhold(
    val skalOpprettes: List<Arbeidsforhold> = emptyList(),
    val skalOppdateres: List<Arbeidsforhold> = emptyList(),
    val skalSlettes: List<Arbeidsforhold> = emptyList(),
)

@Service
@Transactional
class ArbeidsforholdInnhentingService(
    private val eksternArbeidsforholdHenter: EksternArbeidsforholdHenter,
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val nowFactory: Supplier<Instant> = Supplier { Instant.now() },
) {
    fun synkroniserArbeidsforholdForPerson(fnr: String): SynkroniserteArbeidsforhold {
        val eksterntArbeidsforhold = eksternArbeidsforholdHenter.hentEksterneArbeidsforholdForPerson(fnr)
        val interneArbeidsforhold = arbeidsforholdRepository.getAllByFnr(fnr)
        val synkroniserteArbeidsforhold = synkroniserArbeidsforhold(interneArbeidsforhold, eksterntArbeidsforhold, now = nowFactory.get())
        lagreSynkroniserteArbeidsforhold(synkroniserteArbeidsforhold)
        return synkroniserteArbeidsforhold
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

    fun slettArbeidsforhold(arbeidsforholdId: String) {
        arbeidsforholdRepository.deleteByArbeidsforholdId(arbeidsforholdId)
    }

    companion object {
        internal fun synkroniserArbeidsforhold(
            interneArbeidsforhold: List<Arbeidsforhold>,
            eksterneArbeidsforhold: List<EksterntArbeidsforhold>,
            now: Instant = Instant.now(),
        ): SynkroniserteArbeidsforhold {
            val eksterneArbeidsforholdVedId = eksterneArbeidsforhold.associateBy { it.arbeidsforholdId }
            val interneArbeidsforholdVedId = interneArbeidsforhold.associateBy { it.arbeidsforholdId }

            val opprettArbeidsforholdId = eksterneArbeidsforholdVedId.keys - interneArbeidsforholdVedId.keys
            val oppdaterArbeidsforholdId = interneArbeidsforholdVedId.keys.intersect(eksterneArbeidsforholdVedId.keys)
            val slettArbeidsforholdId = interneArbeidsforholdVedId.keys - eksterneArbeidsforholdVedId.keys

            val opprettArbeidsforhold =
                opprettArbeidsforholdId.map { arbeidsforholdId ->
                    val eksterntArbeidsforhold = eksterneArbeidsforholdVedId[arbeidsforholdId]!!
                    Arbeidsforhold(
                        arbeidsforholdId = eksterntArbeidsforhold.arbeidsforholdId,
                        fnr = eksterntArbeidsforhold.fnr,
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
                oppdaterArbeidsforholdId.map { arbeidsforholdId ->
                    val interntArbeidsforhold = interneArbeidsforholdVedId[arbeidsforholdId]!!
                    val eksterntArbeidsforhold = eksterneArbeidsforholdVedId[arbeidsforholdId]!!
                    interntArbeidsforhold.copy(
                        arbeidsforholdId = eksterntArbeidsforhold.arbeidsforholdId,
                        fnr = eksterntArbeidsforhold.fnr,
                        orgnummer = eksterntArbeidsforhold.orgnummer,
                        juridiskOrgnummer = eksterntArbeidsforhold.juridiskOrgnummer,
                        orgnavn = eksterntArbeidsforhold.orgnavn,
                        fom = eksterntArbeidsforhold.fom,
                        tom = eksterntArbeidsforhold.tom,
                        arbeidsforholdType = eksterntArbeidsforhold.arbeidsforholdType,
                    )
                }

            val slettedeArbeidsforhold =
                slettArbeidsforholdId.map { arbeidsforholdId ->
                    interneArbeidsforholdVedId[arbeidsforholdId]!!
                }

            val opprettArbeidsforholdNyeNok = opprettArbeidsforhold.filter { harVaertAnsattSiste4Mnd(it.tom, now = now) }

            return SynkroniserteArbeidsforhold(
                skalOpprettes = opprettArbeidsforholdNyeNok,
                skalOppdateres = oppdaterteArbeidsforhold,
                skalSlettes = slettedeArbeidsforhold,
            )
        }

        // TODO sjekk logikk for dette
        private fun harVaertAnsattSiste4Mnd(
            sluttDato: LocalDate?,
            now: Instant,
        ): Boolean {
            val ansettelsesperiodeFom = LocalDate.ofInstant(now, UTC).minusMonths(4)
            return sluttDato == null || sluttDato.isAfter(ansettelsesperiodeFom)
        }
    }
}
