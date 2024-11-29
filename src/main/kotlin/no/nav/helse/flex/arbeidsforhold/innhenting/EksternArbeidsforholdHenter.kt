package no.nav.helse.flex.arbeidsforhold.innhenting

data class EksterntArbeidsforhold(
    val id: String? = null,
)

class EksternArbeidsforholdHenter {
    fun hentEksterntArbeidsforhold(arbeidsforholdId: String): EksterntArbeidsforhold {
        return EksterntArbeidsforhold()
    }
}
