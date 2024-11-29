package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.AaregClient
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

class EksternArbeidsforholdHenter(
    private val aaregClient: AaregClient,
) {
    fun hentEksterntArbeidsforhold(arbeidsforholdId: String): EksterntArbeidsforhold {
        return EksterntArbeidsforhold(
            arbeidsforholdId = arbeidsforholdId,
            fnr = "",
            orgnummer = "",
            juridiskOrgnummer = "",
            orgnavn = "",
            fom = LocalDate.now(),
            tom = null,
            arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
        )
    }
}
