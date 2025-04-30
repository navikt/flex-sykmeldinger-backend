package no.nav.helse.flex.arbeidsgiverdetaljer

import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsgiverdetaljer.domain.ArbeidsgiverDetaljer
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.config.tilNorgeLocalDate
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.narmesteleder.domain.NarmesteLeder
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.util.function.Supplier

@Service
class ArbeidsgiverDetaljerService(
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val narmeseteLederRepository: NarmesteLederRepository,
    private val nowFactory: Supplier<Instant>,
) {
    fun hentArbeidsgiverDetaljerForPerson(
        identer: PersonIdenter,
        periode: Pair<LocalDate, LocalDate>? = null,
    ): List<ArbeidsgiverDetaljer> {
        val arbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(identer.alle())
        val filtrerteArbeidsforhold = periode?.let { arbeidsforhold.filtrerInnenPeriode(it) } ?: arbeidsforhold

        val narmesteLedere = narmeseteLederRepository.findAllByBrukerFnrIn(identer.alle())

        return sammenstillArbeidsgiverDetaljer(
            arbeidsforhold = filtrerteArbeidsforhold,
            narmesteLedere = narmesteLedere,
            idagProvider = { nowFactory.get().tilNorgeLocalDate() },
        )
    }

    companion object {
        fun sammenstillArbeidsgiverDetaljer(
            arbeidsforhold: Iterable<Arbeidsforhold>,
            narmesteLedere: Iterable<NarmesteLeder>,
            idagProvider: () -> LocalDate,
        ): List<ArbeidsgiverDetaljer> {
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
                    ArbeidsgiverDetaljer(
                        orgnummer = arbeidsforhold.orgnummer,
                        juridiskOrgnummer = arbeidsforhold.juridiskOrgnummer,
                        navn = arbeidsforhold.orgnavn,
                        aktivtArbeidsforhold = erAktivtArbeidsforhold(arbeidsforhold, idag = idagProvider()),
                        fom = arbeidsforhold.fom,
                        tom = arbeidsforhold.tom,
                        naermesteLeder = filtrerNarmesteLedereForArbeidsforhold(narmesteLedere, arbeidsforhold),
                    )
                }
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
                .filter { it.orgnummer == arbeidsforhold.orgnummer && it.narmesteLederNavn != null }
                .maxByOrNull { it.aktivFom }

        private fun Pair<LocalDate, LocalDate?>.overlapperMed(periode: Pair<LocalDate, LocalDate>): Boolean =
            this.erIPeriode(periode.first) || this.erIPeriode(periode.second)

        private fun Pair<LocalDate, LocalDate?>.inneholder(dag: LocalDate): Boolean = this.erIPeriode(dag)

        private fun Pair<LocalDate, LocalDate?>.erIPeriode(date: LocalDate): Boolean {
            val (fom, tom) = this
            return (tom == null || tom >= date) && fom <= date
        }
    }
}
