package no.nav.helse.flex.api

import no.nav.helse.flex.api.dto.AnnenFraverGrunnDTO
import no.nav.helse.flex.api.dto.AnnenFraversArsakDTO
import no.nav.helse.flex.api.dto.DiagnoseDTO
import no.nav.helse.flex.api.dto.SykmeldingsperiodeDTO
import no.nav.helse.flex.config.IdentService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.Month

@Service
class SykmeldingRegelAvklaringer(
    private val identService: IdentService,
) {
    internal fun erOverSyttiAar(
        pasientFnr: String,
        fom: LocalDate,
    ): Boolean {
        val foedselsdato = identService.hentFoedselsdato(pasientFnr)
        return foedselsdato.plusYears(70).isBefore(fom)
    }

    internal fun harRedusertArbeidsgiverperiode(
        hovedDiagnose: DiagnoseDTO?,
        biDiagnoser: List<DiagnoseDTO>,
        sykmeldingsperioder: List<SykmeldingsperiodeDTO>,
        annenFraversArsakDTO: AnnenFraversArsakDTO?,
    ): Boolean {
        val diagnoserSomGirRedusertArbgiverPeriode = listOf("R991", "U071", "U072", "A23", "R992")

        val sykmeldingsperioderInnenforKoronaregler =
            sykmeldingsperioder.filter { periodeErInnenforKoronaregler(it.fom, it.tom) }
        if (sykmeldingsperioderInnenforKoronaregler.isEmpty()) {
            return false
        }
        if (
            hovedDiagnose != null &&
            diagnoserSomGirRedusertArbgiverPeriode.contains(hovedDiagnose.kode)
        ) {
            return true
        } else if (
            biDiagnoser.isNotEmpty() &&
            biDiagnoser.find { diagnoserSomGirRedusertArbgiverPeriode.contains(it.kode) } !=
            null
        ) {
            return true
        }
        return checkSmittefare(annenFraversArsakDTO)
    }

    private fun checkSmittefare(annenFraversArsakDTO: AnnenFraversArsakDTO?) =
        annenFraversArsakDTO?.grunn?.any { annenFraverGrunn ->
            annenFraverGrunn == AnnenFraverGrunnDTO.SMITTEFARE
        } == true

    private fun periodeErInnenforKoronaregler(
        fom: LocalDate,
        tom: LocalDate,
    ): Boolean {
        val koronaForsteFraDato = LocalDate.of(2020, Month.MARCH, 15)
        val koronaForsteTilDato = LocalDate.of(2021, Month.OCTOBER, 1)
        val koronaAndreFraDato = LocalDate.of(2021, Month.NOVEMBER, 30)
        val koronaAndreTilDato = LocalDate.of(2022, Month.JULY, 1)

        return (fom.isAfter(koronaAndreFraDato) && fom.isBefore(koronaAndreTilDato)) ||
            (fom.isBefore(koronaForsteTilDato) && tom.isAfter(koronaForsteFraDato))
    }
}
