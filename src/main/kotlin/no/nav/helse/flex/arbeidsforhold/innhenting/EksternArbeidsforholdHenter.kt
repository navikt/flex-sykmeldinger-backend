package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.arbeidsforhold.innhenting.aaregclient.*
import no.nav.helse.flex.arbeidsforhold.innhenting.eregclient.EregClient
import org.springframework.stereotype.Component
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

@Component
class EksternArbeidsforholdHenter(
    private val aaregClient: AaregClient,
    private val eregClient: EregClient,
) {
    fun hentEksterneArbeidsforholdForPerson(fnr: String): List<EksterntArbeidsforhold> {
        val result = aaregClient.getArbeidsforholdoversikt(fnr)

        return result.arbeidsforholdoversikter
            .filter { it.arbeidssted.type == ArbeidsstedType.Underenhet }
            .map { arbeidsforholdOversikt ->
                val orgnummer = getOrgnummerFraArbeidssted(arbeidsforholdOversikt.arbeidssted)
                val orgNokkelinfo = eregClient.hentNokkelinfo(orgnummer)
                val orgnavn = orgNokkelinfo.navn.sammensattnavn
                EksterntArbeidsforhold(
                    arbeidsforholdId = arbeidsforholdOversikt.navArbeidsforholdId,
                    fnr = getFnrFraArbeidstaker(arbeidsforholdOversikt.arbeidstaker),
                    orgnummer = orgnummer,
                    juridiskOrgnummer = getJuridiskOrgnummerFraOpplysningspliktig(arbeidsforholdOversikt.opplysningspliktig),
                    orgnavn = orgnavn,
                    fom = arbeidsforholdOversikt.startdato,
                    tom = arbeidsforholdOversikt.sluttdato,
                    arbeidsforholdType = parseArbeidsforholdType(arbeidsforholdOversikt.type.kode),
                )
            }
    }
}

fun getFnrFraArbeidstaker(arbeidstaker: Arbeidstaker): String {
    val gjeldendePersonIdenter =
        arbeidstaker.identer
            .filter { it.type in setOf(IdentType.AKTORID, IdentType.FOLKEREGISTERIDENT) }
            .filter { it.gjeldende == true }
    require(gjeldendePersonIdenter.isNotEmpty()) { "Ingen gjeldende identer inneholder fnr" }

    return gjeldendePersonIdenter.first().ident
}

fun getOrgnummerFraArbeidssted(arbeidssted: Arbeidssted): String {
    return arbeidssted.identer.first { it.type == IdentType.ORGANISASJONSNUMMER }.ident
}

fun getJuridiskOrgnummerFraOpplysningspliktig(opplysningspliktig: Opplysningspliktig): String {
    return opplysningspliktig.identer.first { it.type == IdentType.ORGANISASJONSNUMMER }.ident
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
