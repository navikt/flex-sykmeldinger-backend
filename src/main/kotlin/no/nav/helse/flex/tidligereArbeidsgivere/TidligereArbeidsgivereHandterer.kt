package no.nav.helse.flex.tidligereArbeidsgivere

import no.nav.helse.flex.api.dto.TidligereArbeidsgiver
import no.nav.helse.flex.sykmelding.Sykmelding
import no.nav.helse.flex.sykmeldinghendelse.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object TidligereArbeidsgivereHandterer {
    fun finnTidligereArbeidsgivere(
        alleSykmeldinger: List<Sykmelding>,
        gjeldendeSykmeldingId: String,
    ): List<TidligereArbeidsgiver> {
        val gjeldendeSykmelding =
            alleSykmeldinger.find { it.sykmeldingId == gjeldendeSykmeldingId }
                ?: throw IllegalArgumentException("Sykmelding med id $gjeldendeSykmeldingId finnes ikke")

        val tidligereSykmeldinger =
            alleSykmeldinger
                .filter { it.fom < gjeldendeSykmelding.fom }
                .filter { it.sykmeldingId != gjeldendeSykmeldingId }
        val innsendteSykmeldinger =
            tidligereSykmeldinger.filter {
                it.sisteHendelse().status in
                    setOf(HendelseStatus.SENDT_TIL_NAV, HendelseStatus.SENDT_TIL_ARBEIDSGIVER)
            }
        val aktuelleSykmeldinger =
            innsendteSykmeldinger.filter {
                it overlapperMed gjeldendeSykmelding ||
                    it erKantIKantMed gjeldendeSykmelding
            }
        val tidligereArbeidsgivere =
            aktuelleSykmeldinger.mapNotNull {
                when (val tilleggsinfo = it.sisteHendelse().tilleggsinfo) {
                    is ArbeidstakerTilleggsinfo -> tilleggsinfo.arbeidsgiver.mapTilTidligereArbeidsgiver()
                    is ArbeidsledigTilleggsinfo -> tilleggsinfo.tidligereArbeidsgiver
                    is PermittertTilleggsinfo -> tilleggsinfo.tidligereArbeidsgiver
                    else -> null
                }
            }
        return tidligereArbeidsgivere

        // 1.a filtrer bort alle etterfølgende sykmeldinger (som har fom etter den gjeldende)
        // 1.b filtrer bort den aktuelle sykmeldingen og alle ikke-innsendte
        // 2. finn de sykmeldingene som har fom før den gjeldende sykmeldingen, og tom kant-i-kant eller etter fom til gjeldene sykmeldingen
        // 3. hent ut arbeidsgiver fra tilleggsinfo, eller tidligere arbeidsgiver fra tilleggsinfo
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

    private fun Arbeidsgiver.mapTilTidligereArbeidsgiver() =
        TidligereArbeidsgiver(
            orgNavn = this.orgnavn,
            orgnummer = this.orgnummer,
        )
}
