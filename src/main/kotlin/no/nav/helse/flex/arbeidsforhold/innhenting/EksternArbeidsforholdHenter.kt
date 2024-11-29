package no.nav.helse.flex.arbeidsforhold.innhenting

data class EksterntArbeidsforhold(
    val arbeidsforholdId: String? = null,
)

class EksternArbeidsforholdHenter {
    fun hentEksterntArbeidsforhold(arbeidsforholdId: String): EksterntArbeidsforhold {
        return EksterntArbeidsforhold()
    }
}
