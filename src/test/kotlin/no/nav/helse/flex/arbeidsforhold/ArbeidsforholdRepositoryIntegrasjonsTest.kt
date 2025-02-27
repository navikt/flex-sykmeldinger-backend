package no.nav.helse.flex.arbeidsforhold

import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.UUID

class ArbeidsforholdRepositoryIntegrasjonsTest : IntegrasjonTestOppsett() {
    @AfterEach
    fun tearDown() {
        slettDatabase()
    }

    @Test
    fun `burde hente arbeidsforhold ved fnr`() {
        val opprettetArbeidsforhold =
            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "1",
                    navArbeidsforholdId = UUID.randomUUID().toString(),
                ),
            )

        val alleLagredeArbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(listOf("1"))
        alleLagredeArbeidsforhold shouldHaveSize 1

        val lagretArbeidsforhold = alleLagredeArbeidsforhold.first()
        opprettetArbeidsforhold `should be equal to` lagretArbeidsforhold
    }

    @Test
    fun `burde hente arbeidsforhold ved alle identer`() {
        val opprettetArbeidsforhold =
            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "1",
                    navArbeidsforholdId = UUID.randomUUID().toString(),
                ),
            )

        val opprettetArbeidsforhold2 =
            arbeidsforholdRepository.save(
                lagArbeidsforhold(
                    fnr = "2",
                    navArbeidsforholdId = UUID.randomUUID().toString(),
                ),
            )

        val alleLagredeArbeidsforhold = arbeidsforholdRepository.getAllByFnrIn(listOf("1", "2"))
        alleLagredeArbeidsforhold shouldHaveSize 2

        alleLagredeArbeidsforhold.first() `should be equal to` opprettetArbeidsforhold
        alleLagredeArbeidsforhold.last() `should be equal to` opprettetArbeidsforhold2
    }
}
