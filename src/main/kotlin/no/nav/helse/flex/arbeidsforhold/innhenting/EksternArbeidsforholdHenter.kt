package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import org.springframework.data.annotation.Id
import java.time.Instant
import java.time.LocalDate

data class EksterntArbeidsforhold(
    val arbeidsforholdId: String,
    val fnr: String,
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val orgnavn: String,
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val arbeidsforholdType: ArbeidsforholdType?,
)

class EksternArbeidsforholdHenter {
    fun hentEksterntArbeidsforhold(arbeidsforholdId: String): EksterntArbeidsforhold {
        return EksterntArbeidsforhold(
            arbeidsforholdId = arbeidsforholdId,
            fnr = "",
            orgnummer = "",
            juridiskOrgnummer = "",
            orgnavn = "",
            fom = LocalDate.now(),
            tom = null,
            arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
        )
    }
}
