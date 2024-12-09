package no.nav.helse.flex.arbeidsforhold.innhenting

import org.springframework.stereotype.Component

@Component
class RegistrertePersonerForArbeidsforhold {
    fun erPersonRegistrert(fnr: String): Boolean {
        // TODO: Hent info fra registrerte sykmeldinger (SykmeldingRepository)
        // Feks.: return sykmeldingRepository.getAllByFnr(fnr).isNotEmpty()
        return true
    }
}
