package no.nav.helse.flex.arbeidsforhold.innhenting

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RegistrertePersonerForArbeidsforhold(
    @Value("\${REGISTRERT_FOR_ARBEIDSFORHOLD}") private val registrertFOrArbeidsforhold: Boolean,
) {
    fun erPersonRegistrert(fnr: String): Boolean {
        // TODO: Hent info fra registrerte sykmeldinger (SykmeldingRepository)
        // Feks.: return sykmeldingRepository.getAllByFnr(fnr).isNotEmpty()
        return registrertFOrArbeidsforhold
    }
}
