package no.nav.helse.flex.virksomhet

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.config.tilNorgeLocalDate
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import no.nav.helse.flex.virksomhet.domain.Virksomhet
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.function.Supplier

@Service
class VirksomhetHenterService(
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val narmeseteLederRepository: NarmesteLederRepository,
    private val nowFactory: Supplier<Instant>,
) {
    fun hentVirksomheterForPerson(identer: PersonIdenter): List<Virksomhet> {
        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(identer.alle())
        val narmesteLedere = narmeseteLederRepository.findAllByBrukerFnrIn(identer.alle())

        val virksomheter =
            sammenstillVirksomheter(
                arbeidsforhold = arbeidsforhold,
                narmesteLedere = narmesteLedere,
                idagProvider = { nowFactory.get().tilNorgeLocalDate() },
            )

        return virksomheter
    }

    fun hentVirksomheterForPersonInnenforPeriode(
        identer: PersonIdenter,
        periode: Pair<LocalDate, LocalDate>,
    ): List<Virksomhet> {
        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(identer.alle())
        val arbeidsforholdInnenPeriode = arbeidsforhold.filtrerInnenPeriode(periode)

        val narmesteLedere = narmeseteLederRepository.findAllByBrukerFnrIn(identer.alle())

        val virksomheter =
            sammenstillVirksomheter(
                arbeidsforholdInnenPeriode,
                narmesteLedere = narmesteLedere,
                idagProvider = { nowFactory.get().tilNorgeLocalDate() },
            )

        return virksomheter
    }

    companion object {
        fun sammenstillVirksomheter(
            arbeidsforhold: Iterable<Arbeidsforhold>,
            narmesteLedere: Iterable<NarmesteLeder>,
            idagProvider: () -> LocalDate,
        ): List<Virksomhet> {
            val gyldigeArbeidsforhold =
                arbeidsforhold.filter { erGyldigArbeidsforholdType(it.arbeidsforholdType) }

            if (gyldigeArbeidsforhold.isEmpty()) {
                return emptyList()
            }

            return gyldigeArbeidsforhold
                .sortedWith(
                    compareByDescending(nullsLast()) { it.tom },
                ).distinctBy { it.orgnummer }
                .map { arbeidsforhold ->
                    Virksomhet(
                        orgnummer = arbeidsforhold.orgnummer,
                        juridiskOrgnummer = arbeidsforhold.juridiskOrgnummer,
                        navn = arbeidsforhold.orgnavn,
                        aktivtArbeidsforhold = erAktivtArbeidsforhold(arbeidsforhold, idag = idagProvider()),
                        fom = arbeidsforhold.fom,
                        tom = arbeidsforhold.tom,
                        naermesteLeder = filtrerNarmesteLedereForArbeidsforhold(narmesteLedere, arbeidsforhold),
                    )
                }.filter { it.naermesteLeder != null }
        }

        fun Iterable<Arbeidsforhold>.filtrerInnenPeriode(periode: Pair<LocalDate, LocalDate>): List<Arbeidsforhold> =
            this.filter { arbeidsforhold ->
                (arbeidsforhold.fom to arbeidsforhold.tom).overlapperMed(periode)
            }

        private fun erGyldigArbeidsforholdType(arbeidsforholdType: ArbeidsforholdType?): Boolean =
            when (arbeidsforholdType) {
                ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_HONORAR_PERSONER_MM -> false
                null -> true
                else -> true
            }

        private fun erAktivtArbeidsforhold(
            arbeidsforhold: Arbeidsforhold,
            idag: LocalDate,
        ): Boolean = (arbeidsforhold.fom to arbeidsforhold.tom).inneholder(idag)

        private fun filtrerNarmesteLedereForArbeidsforhold(
            narmesteLedere: Iterable<NarmesteLeder>,
            arbeidsforhold: Arbeidsforhold,
        ): NarmesteLeder? =
            narmesteLedere
                .filter { it.orgnummer == arbeidsforhold.orgnummer }
                .sortedBy { it.aktivFom }
                .lastOrNull()

        private fun Pair<LocalDate, LocalDate?>.overlapperMed(periode: Pair<LocalDate, LocalDate>): Boolean {
            val (fom, tom) = this
            val (periodeFom, periodeTom) = periode

            val tomErInnenfor = tom == null || tom >= periodeFom
            val fomErInnenfor = fom <= periodeTom
            return tomErInnenfor && fomErInnenfor
        }

        private fun Pair<LocalDate, LocalDate?>.inneholder(dag: LocalDate): Boolean {
            val (fom, tom) = this

            val tomErInnenfor = tom == null || tom >= dag
            val fomErInnenfor = fom <= dag
            return tomErInnenfor && fomErInnenfor
        }
    }
}
