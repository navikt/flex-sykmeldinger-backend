package no.nav.helse.flex.tidligereArbeidsgivere

import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.sykmelding.domain.HendelseStatus
import no.nav.helse.flex.sykmelding.domain.Sykmelding
import no.nav.helse.flex.sykmelding.domain.tsm.Aktivitet
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class TidligereArbeidsgivereHandterer {
    companion object {
        // tidligere arbeidsgiver brukes kun når bruker har valgt arbeidsledig/permittert, for de kan ha startet sykmeldingen når de var ansatt
        // og kan så fortsette å være sykmeldt, og da skal det fortsatt være arbeisdtagersøknaded
        // så da trenger de å finne en sykmelding som er sendt til arbeidsgiver tidligere i sykefraværstilfellet
        // dette er en sykmelding der det er kant til kant ... en arbeidag imellom teller, om det er helg imellom "teller det ikke"
        // lørdag og søndag tas med, de tar ikke med helligdager
        fun finnTidligereArbeidsgivere(
            alleSykmeldinger: List<Sykmelding>,
            gjeldendeSykmeldingId: String,
        ): List<TidligereArbeidsgiver> {
            val gjeldendeSykmelding = alleSykmeldinger.first { it.sykmeldingId == gjeldendeSykmeldingId }
            val sykmeldingerMedUnikeOrgnummer =
                alleSykmeldinger
                    .filterNot { it.sykmeldingId == gjeldendeSykmeldingId }
                    .filter {
                        it.sisteHendelse().status == HendelseStatus.SENDT_TIL_ARBEIDSGIVER
                    }.filter {
                        val forsteFom = finnForsteFom(gjeldendeSykmelding.sykmeldingGrunnlag.aktivitet)
                        val tidligereArbeidsgiverType =
                            finnTidligereArbeidsgiverType(forsteFom, it.sykmeldingGrunnlag.aktivitet)
                        tidligereArbeidsgiverType != TidligereArbeidsgiverType.INGEN
                    }.distinctBy {
                        it
                            .sisteHendelse()
                            .arbeidstakerInfo
                            ?.arbeidsgiver
                            ?.orgnummer
                    }
            return sykmeldingerMedUnikeOrgnummer.map { sykmelding ->
                val arbeidsgiverForSisteHendelse = sykmelding.sisteHendelse().arbeidstakerInfo?.arbeidsgiver
                TidligereArbeidsgiver(
                    orgNavn = arbeidsgiverForSisteHendelse?.orgnavn ?: "Ukjent",
                    orgnummer = arbeidsgiverForSisteHendelse?.orgnummer ?: "Ukjent",
                )
            }
        }

        private fun finnTidligereArbeidsgiverType(
            forsteFom: LocalDate,
            sykmeldingsperioder: List<Aktivitet>,
        ): TidligereArbeidsgiverType {
            val kantTilKant = sisteTomIKantMedDag(sykmeldingsperioder, forsteFom)
            if (kantTilKant) return TidligereArbeidsgiverType.KANT_TIL_KANT
            val overlappende =
                erOverlappende(
                    tidligereSmTom = sykmeldingsperioder.maxOf { it.tom },
                    tidligereSmFom = sykmeldingsperioder.minOf { it.fom },
                    fom = forsteFom,
                )
            if (overlappende) return TidligereArbeidsgiverType.OVERLAPPENDE
            return TidligereArbeidsgiverType.INGEN
        }

        private fun erOverlappende(
            tidligereSmTom: LocalDate,
            tidligereSmFom: LocalDate,
            fom: LocalDate,
        ) = (
            fom.isAfter(tidligereSmFom) &&
                fom.isBefore(
                    tidligereSmTom.plusDays(1),
                )
        )

        private fun sisteTomIKantMedDag(
            perioder: List<Aktivitet>,
            dag: LocalDate,
        ): Boolean {
            val sisteTom =
                perioder.maxByOrNull { it.tom }?.tom
                    ?: throw IllegalStateException("Skal ikke kunne ha periode uten tom")
            return !erArbeidsDagIMellom(sisteTom, dag)
        }

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

        private fun finnForsteFom(perioder: List<Aktivitet>): LocalDate =
            perioder.minByOrNull { it.fom }?.fom
                ?: throw IllegalStateException("Skal ikke kunne ha periode uten fom")
    }
}

enum class TidligereArbeidsgiverType {
    KANT_TIL_KANT,
    OVERLAPPENDE,
    INGEN,
}
