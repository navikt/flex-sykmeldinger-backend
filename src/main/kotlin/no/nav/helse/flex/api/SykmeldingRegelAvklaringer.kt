package no.nav.helse.flex.api

import no.nav.helse.flex.config.IdentService
import org.springframework.stereotype.Service
import java.time.LocalDate

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
}
