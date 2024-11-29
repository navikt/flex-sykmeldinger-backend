package no.nav.helse.flex.arbeidsforhold

import org.springframework.data.repository.CrudRepository

interface ArbeidsforholdRepository : CrudRepository<Arbeidsforhold, String> {
    fun getAllByFnr(fnr: String): List<Arbeidsforhold>

    fun findByArbeidsforholdId(arbeidsforholdId: String): Arbeidsforhold
}
