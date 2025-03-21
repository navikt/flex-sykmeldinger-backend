package no.nav.helse.flex.tidligereArbeidsgivere

import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class TidligereArbeidsgivereHandterer {
    companion object {
        fun finnTidligereArbeidsgivere(
            alleSykmeldinger: List<Sykmelding>,
            gjeldendeSykmeldingId: String,
        ): List<TidligereArbeidsgiver> {
            val sammenhengendeSykmeldinger = settSammenhengendeSykmeldinger(alleSykmeldinger)
            val unikeArbeidsgivere =
                sammenhengendeSykmeldinger
                    .takeWhile { it.sykmeldingId != gjeldendeSykmeldingId }
                    .filter {
                        it.sisteHendelse().status == HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    }.distinctBy {
                        it
                            .sisteHendelse()
                            .arbeidstakerInfo
                            ?.arbeidsgiver
                            ?.orgnummer
                    }.map { sykmelding ->
                        val arbeidsgiverForSisteHendelse = sykmelding.sisteHendelse().arbeidstakerInfo?.arbeidsgiver
                        TidligereArbeidsgiver(
                            orgNavn = arbeidsgiverForSisteHendelse?.orgnavn ?: "Ukjent",
                            orgnummer = arbeidsgiverForSisteHendelse?.orgnummer ?: "Ukjent",
                        )
                    }

            return unikeArbeidsgivere
        }

        private fun settSammenhengendeSykmeldinger(sykmeldinger: List<Sykmelding>): List<Sykmelding> {
            val sammenhengendeSykmeldinger = mutableListOf<Sykmelding>()
            var etterfolgendeSykmelding: Sykmelding? = null

            sykmeldinger.sortedWith(compareByDescending<Sykmelding> { it.tom }.thenByDescending { it.fom }).forEach { sykmelding ->
                etterfolgendeSykmelding?.let { etterfolgende ->
                    if (sykmelding erKantIKantMed etterfolgende || sykmelding overlapperMed etterfolgende) {
                        sammenhengendeSykmeldinger.add(sykmelding)
                    }
                }
                etterfolgendeSykmelding = sykmelding
            }
            return sammenhengendeSykmeldinger.sortedBy { it.tom }
        }

        private infix fun Sykmelding.overlapperMed(other: Sykmelding): Boolean = this.fom..this.tom overlapper other.fom..other.tom

        private infix fun ClosedRange<LocalDate>.overlapper(other: ClosedRange<LocalDate>): Boolean =
            this.start <= other.endInclusive && other.start <= this.endInclusive

        private infix fun Sykmelding.erKantIKantMed(other: Sykmelding): Boolean = !erArbeidsDagIMellom(this.tom, other.fom)

        private fun erArbeidsDagIMellom(
            tom: LocalDate,
            fom: LocalDate,
        ): Boolean {
            val daysBetween = ChronoUnit.DAYS.between(tom, fom).toInt()
            if (daysBetween < 0) return true
            return when (fom.dayOfWeek) {
                DayOfWeek.MONDAY -> daysBetween > 3
                DayOfWeek.SUNDAY -> daysBetween > 2
                else -> daysBetween > 1
            }
        }
    }
}
