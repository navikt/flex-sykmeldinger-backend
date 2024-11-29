package no.nav.helse.flex.arbeidsforhold.innhenting

data class EksterntArbeidsforhold(
    val arbeidsforholdId: String,
)

class EksternArbeidsforholdHenter {
    fun hentEksterntArbeidsforhold(arbeidsforholdId: String): EksterntArbeidsforhold {
        return EksterntArbeidsforhold("")
    }
}
