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
                    if (sykmelding `er kant i kant med` etterfolgende || sykmelding `overlapper med` etterfolgende) {
                        sammenhengendeSykmeldinger.add(sykmelding)
                    }
                }
                etterfolgendeSykmelding = sykmelding
            }
            return sammenhengendeSykmeldinger.sortedBy { it.tom }
        }

        @Suppress("ktlint:standard:function-naming")
        private infix fun Sykmelding.`overlapper med`(other: Sykmelding): Boolean = this.fom..this.tom overlapper other.fom..other.tom

        private infix fun ClosedRange<LocalDate>.overlapper(other: ClosedRange<LocalDate>): Boolean {
            val symeldingDatoer = this.toList()
            val etterfolgendeDatoer = other.toList()
            return symeldingDatoer.any { date -> other.contains(date) } || etterfolgendeDatoer.any { date -> this.contains(date) }
        }

        private fun ClosedRange<LocalDate>.toList(): List<LocalDate> {
            val list = mutableListOf<LocalDate>()
            var currentDate = this.start

            while (!currentDate.isAfter(this.endInclusive)) {
                list.add(currentDate)
                currentDate = currentDate.plusDays(1)
            }

            return list
        }

        @Suppress("ktlint:standard:function-naming")
        private infix fun Sykmelding.`er kant i kant med`(other: Sykmelding): Boolean = !erArbeidsDagIMellom(this.tom, other.fom)

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
