package no.nav.helse.flex.virksomhet

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.virksomhet.domain.Virksomhet
import org.springframework.stereotype.Service

@Service
class VirksomhetService(
    private val arbeidsforholdRepository: ArbeidsforholdRepository,
    private val narmeseteLederRepository: NarmesteLederRepository,
) {
    fun hentVirksomheterForPerson(fnr: String): List<Virksomhet> {
        val arbeidsforhold = arbeidsforholdRepository.getAllByFnr(fnr)

        val gyldigeArbeidsforhold =
            arbeidsforhold.filter {
                it.arbeidsforholdType == ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_HONORAR_PERSONER_MM
            }

        if (arbeidsforhold.isEmpty()) {
            return emptyList()
        }

//        val arbeidsgivereWithinSykmeldingsperiode =
//            filterArbeidsgivere(sykmeldingFom, sykmeldingTom, arbeidsgivere)

        val narmesteLedere = narmeseteLederRepository.findAllByBrukerFnr(fnr)

        val virksomheter =
            arbeidsforhold.map { arbeidsforhold ->
                val narmesteLeder = narmesteLedere.find { it.orgnummer == arbeidsforhold.orgnummer }
                Virksomhet(
                    orgnummer = arbeidsforhold.orgnummer,
                    juridiskOrgnummer = arbeidsforhold.juridiskOrgnummer,
                    navn = arbeidsforhold.orgnavn,
                    aktivtArbeidsforhold = false,
                    naermesteLeder = narmesteLeder,
                )
            }

        return emptyList()
    }

    private fun pureFunc(
        arbeidsforhold: String,
        nærmesteLedere: String,
    ) {
        // Pure
    }

//    suspend fun getArbeidsgivere(
//        fnr: String,
//        date: LocalDate = LocalDate.now(),
//    ): List<Arbeidsgiverinfo> {
//        val arbeidsgivere = getArbeidsforhold(fnr = fnr)
//
//        if (arbeidsgivere.isEmpty()) {
//            return emptyList()
//        }
//        val aktiveNarmesteledere = narmestelederDb.getNarmesteleder(fnr)
//
//        return arbeidsgivere
//            .sortedWith(
//                compareByDescending(nullsLast()) { it.tom },
//            ).distinctBy { it.orgnummer }
//            .map { arbeidsforhold -> arbeidsgiverinfo(aktiveNarmesteledere, arbeidsforhold, date) }
//    }
//
//    private fun arbeidsgiverinfo(
//        aktiveNarmesteledere: List<NarmestelederDbModel>,
//        arbeidsforhold: Arbeidsforhold,
//        date: LocalDate,
//    ): Arbeidsgiverinfo {
//        val narmesteLeder = aktiveNarmesteledere.find { it.orgnummer == arbeidsforhold.orgnummer }
//        return Arbeidsgiverinfo(
//            orgnummer = arbeidsforhold.orgnummer,
//            juridiskOrgnummer = arbeidsforhold.juridiskOrgnummer,
//            navn = arbeidsforhold.orgNavn,
//            aktivtArbeidsforhold =
//                arbeidsforhold.tom == null ||
//                    !date.isAfter(arbeidsforhold.tom) &&
//                    !date.isBefore(arbeidsforhold.fom),
//            naermesteLeder = narmesteLeder?.tilNarmesteLeder(arbeidsforhold.orgNavn),
//        )
//    }
//
//    private fun NarmestelederDbModel.tilNarmesteLeder(orgnavn: String): NarmesteLeder =
//        NarmesteLeder(
//            navn = navn,
//            orgnummer = orgnummer,
//            organisasjonsnavn = orgnavn,
//        )
//
//    private fun filterArbeidsgivere(
//        sykmeldingFom: LocalDate,
//        sykmeldingTom: LocalDate,
//        allArbeidsgivere: List<Arbeidsforhold>,
//    ): List<Arbeidsforhold> =
//        allArbeidsgivere.filter {
//            isArbeidsforholdWithinSykmeldingPeriode(it, sykmeldingFom, sykmeldingTom)
//        }
//
//    private fun isArbeidsforholdWithinSykmeldingPeriode(
//        arbeidsforhold: Arbeidsforhold,
//        sykmeldingFom: LocalDate,
//        sykmeldingTom: LocalDate,
//    ): Boolean {
//        val checkSluttdato =
//            arbeidsforhold.tom == null ||
//                arbeidsforhold.tom.isAfter(sykmeldingFom) ||
//                arbeidsforhold.tom == sykmeldingFom
//        val checkStartdato =
//            arbeidsforhold.fom.isBefore(sykmeldingTom) || arbeidsforhold.fom == sykmeldingTom
//        return checkStartdato && checkSluttdato
//    }
}
