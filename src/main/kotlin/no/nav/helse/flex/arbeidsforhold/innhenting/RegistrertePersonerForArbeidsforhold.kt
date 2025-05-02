package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.config.IdentService
import no.nav.helse.flex.sykmelding.domain.ISykmeldingRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class RegistrertePersonerForArbeidsforhold(
    @Value("\${REGISTRERT_FOR_ARBEIDSFORHOLD}") private val registrertForArbeidsforhold: Boolean,
    private val identService: IdentService,
    private val sykmeldingRepository: ISykmeldingRepository,
) {
    fun erPersonRegistrert(fnr: String): Boolean {
        val identer = identService.hentFolkeregisterIdenterMedHistorikkForFnr(fnr)
        val personHarSykmelding = sykmeldingRepository.findAllByPersonIdenter(identer).isNotEmpty()
        return personHarSykmelding && registrertForArbeidsforhold
    }
}
