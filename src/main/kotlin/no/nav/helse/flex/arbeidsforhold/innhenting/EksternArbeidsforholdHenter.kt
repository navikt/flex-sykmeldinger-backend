package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.AaregClient
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.Arbeidssted
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.Opplysningspliktig
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
                orgnummer = getOrgnummerFraArbeidssted(arbeidsforholdOversikt.arbeidssted),
                juridiskOrgnummer = getJuridiskOrgnummerFraOpplysningspliktig(arbeidsforholdOversikt.opplysningspliktig),
                orgnavn = "",
                fom = arbeidsforholdOversikt.startdato,
                tom = arbeidsforholdOversikt.sluttdato,
                arbeidsforholdType = parseArbeidsforholdType(arbeidsforholdOversikt.type.kode),
            )
        }
    }
}

fun getOrgnummerFraArbeidssted(arbeidssted: Arbeidssted): String {
    return arbeidssted.identer.first { it.type == "ORGANISASJONSNUMMER" }.ident
}

fun getJuridiskOrgnummerFraOpplysningspliktig(opplysningspliktig: Opplysningspliktig): String {
    return opplysningspliktig.identer.first { it.type == "ORGANISASJONSNUMMER" }.ident
}

fun parseArbeidsforholdType(kode: String): ArbeidsforholdType {
    return when (kode) {
        "forenkletOppgjoersordning" -> ArbeidsforholdType.FORENKLET_OPPGJOERSORDNING
        "frilanserOppdragstakerHonorarPersonerMm" ->
            ArbeidsforholdType.FRILANSER_OPPDRAGSTAKER_HONORAR_PERSONER_MM

        "maritimtArbeidsforhold" -> ArbeidsforholdType.MARITIMT_ARBEIDSFORHOLD
        "ordinaertArbeidsforhold" -> ArbeidsforholdType.ORDINAERT_ARBEIDSFORHOLD
        "pensjonOgAndreTyperYtelserUtenAnsettelsesforhold" ->
            ArbeidsforholdType.PENSJON_OG_ANDRE_TYPER_YTELSER_UTEN_ANSETTELSESFORHOLD

        else -> throw IllegalArgumentException("Ugyldig arbeidsforhold type $kode")
    }
}
