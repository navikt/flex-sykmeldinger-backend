package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ArbeidsforholdRepositoryIntegrasjonsTest : FellesTestOppsett() {
    @Autowired
    lateinit var arbeidsforholdRepository: ArbeidsforholdRepository

    @Test
    fun `burde hente arbeidsforhold ved fnr`() {
        val opprettetArbeidsforhold =
            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "1",
                ),
            )

        val alleLagredeArbeidsforhold = arbeidsforholdRepository.getAllByFnr("1")
        alleLagredeArbeidsforhold shouldHaveSize 1

        val lagretArbeidsforhold = alleLagredeArbeidsforhold.first()
        opprettetArbeidsforhold `should be equal to` lagretArbeidsforhold
    }
}
