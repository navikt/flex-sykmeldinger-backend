package no.nav.helse.flex.arbeidsforhold

import org.springframework.data.repository.CrudRepository

interface ArbeidsforholdRepository : CrudRepository<Arbeidsforhold, String> {
    fun findByNavArbeidsforholdId(navArbeidsforholdId: String): Arbeidsforhold?

    fun deleteByNavArbeidsforholdId(navArbeidsforholdId: String)

    fun getAllByFnrIn(identer: List<String>): List<Arbeidsforhold>
}
