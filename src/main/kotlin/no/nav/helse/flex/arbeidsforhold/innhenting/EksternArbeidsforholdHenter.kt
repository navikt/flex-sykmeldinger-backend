package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.AaregClient
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.Arbeidssted
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
    fun hentEksterneArbeidsforholdForPerson(fnr: String): List<EksterntArbeidsforhold> {
        val result = aaregClient.getArbeidsforholdoversikt(fnr)


        return result.arbeidsforholdoversikter.map { arbeidsforholdOversikt ->
            EksterntArbeidsforhold(
                arbeidsforholdId = arbeidsforholdOversikt.navArbeidsforholdId,
                fnr = arbeidsforholdOversikt.arbeidstaker.identer.first().ident,
                orgnummer = arbeidsforholdOversikt.arbeidssted.identer.first().ident,
                juridiskOrgnummer = arbeidsforholdOversikt.opplysningspliktig.identer.first().ident,
                orgnavn = "",
                fom = arbeidsforholdOversikt.startdato,
                tom = arbeidsforholdOversikt.sluttdato,
                arbeidsforholdType = ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD,
            )
        }
    }
}

fun getOrgnummerFraArbeidssted(arbeidssted: Arbeidssted): String {
    return arbeidssted.identer.first { it.type == "ORGANISASJONSNUMMER" }.ident
}
