package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.arbeidsforhold.Arbeidsforhold
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class ArbeidsforholdRepositoryIntegrasjonsTest : FellesTestOppsett() {
    @Autowired
    lateinit var arbeidsforholdRepository: ArbeidsforholdRepository

    @Test
    fun `burde hente arbeidsforhold ved fnr`() {
        val opprettetArbeidsforhold = arbeidsforholdRepository.save(lagArbeidsforhold(
            fnr = "1"
        ))

        val alleLagredeArbeidsforhold = arbeidsforholdRepository.getAllByFnr("1")
        alleLagredeArbeidsforhold shouldHaveSize 1

        val lagretArbeidsforhold = alleLagredeArbeidsforhold.first()
        opprettetArbeidsforhold `should be equal to` lagretArbeidsforhold
    }

    private fun lagArbeidsforhold(
        id: String? = null,
        fnr: String = "00000000001",
        orgnummer: String = "org",
        juridiskOrgnummer: String = "org",
        orgnavn: String = "Org",
        fom: LocalDate = LocalDate.parse("2020-01-01"),
        tom: LocalDate? = null,
        arbeidsforholdType: ArbeidsforholdType? = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
    ): Arbeidsforhold {
        return Arbeidsforhold(
            id = id,
            fnr = fnr,
            orgnummer = orgnummer,
            juridiskOrgnummer = juridiskOrgnummer,
            orgnavn = orgnavn,
            fom = fom,
            tom = tom,
            arbeidsforholdType = arbeidsforholdType,
        )

    }
}
