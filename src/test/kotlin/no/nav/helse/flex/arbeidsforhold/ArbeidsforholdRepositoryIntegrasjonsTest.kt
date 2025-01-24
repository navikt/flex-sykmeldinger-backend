package no.nav.helse.flex.arbeidsforhold

import no.nav.helse.flex.FellesTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.util.UUID

class ArbeidsforholdRepositoryIntegrasjonsTest : FellesTestOppsett() {
    @Test
    fun `burde hente arbeidsforhold ved fnr`() {
        val opprettetArbeidsforhold =
            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "1",
                    navArbeidsforholdId = UUID.randomUUID().toString(),
                ),
            )

        val alleLagredeArbeidsforhold = arbeidsforholdRepository.getAllByFnr("1")
        alleLagredeArbeidsforhold shouldHaveSize 1

        val lagretArbeidsforhold = alleLagredeArbeidsforhold.first()
        opprettetArbeidsforhold `should be equal to` lagretArbeidsforhold
    }
}
