package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.clients.aareg.*
import no.nav.helse.flex.clients.ereg.EregClient
import no.nav.helse.flex.config.PersonIdenter
import org.springframework.stereotype.Component
import java.time.LocalDate

data class EksterntArbeidsforhold(
    val navArbeidsforholdId: String,
    val orgnummer: String,
    val juridiskOrgnummer: String,
    val orgnavn: String,
    val fom: LocalDate,
    val tom: LocalDate? = null,
    val arbeidsforholdType: ArbeidsforholdType?,
)

data class IdenterOgEksterneArbeidsforhold(
    val identer: PersonIdenter,
    val eksterneArbeidsforhold: List<EksterntArbeidsforhold>,
)

@Component
class EksternArbeidsforholdHenter(
    private val aaregClient: AaregClient,
    private val eregClient: EregClient,
) {
    fun hentEksterneArbeidsforholdForPerson(fnr: String): IdenterOgEksterneArbeidsforhold {
        val result = aaregClient.getArbeidsforholdoversikt(fnr)

        val eksterneArbeidsforhold =
            result.arbeidsforholdoversikter
                .filter { it.arbeidssted.type == ArbeidsstedType.Underenhet }
                .map { arbeidsforholdOversikt ->
                    val orgnummer = getOrgnummerFraArbeidssted(arbeidsforholdOversikt.arbeidssted)
                    val orgNokkelinfo = eregClient.hentNokkelinfo(orgnummer)
                    val orgnavn = orgNokkelinfo.navn.sammensattnavn
                    EksterntArbeidsforhold(
                        navArbeidsforholdId = arbeidsforholdOversikt.navArbeidsforholdId,
                        orgnummer = orgnummer,
                        juridiskOrgnummer = getJuridiskOrgnummerFraOpplysningspliktig(arbeidsforholdOversikt.opplysningspliktig),
                        orgnavn = orgnavn,
                        fom = arbeidsforholdOversikt.startdato,
                        tom = arbeidsforholdOversikt.sluttdato,
                        arbeidsforholdType = parseArbeidsforholdType(arbeidsforholdOversikt.type.kode),
                    )
                }
        val identer =
            PersonIdenter(
                originalIdent = fnr,
                andreIdenter =
                    result.arbeidsforholdoversikter
                        .flatMap { arbeidsforhold ->
                            arbeidsforhold.arbeidstaker.identer.map { it.ident }
                        }.distinct(),
            )

        return IdenterOgEksterneArbeidsforhold(
            identer = identer,
            eksterneArbeidsforhold = eksterneArbeidsforhold,
        )
    }

    companion object {
        fun getOrgnummerFraArbeidssted(arbeidssted: Arbeidssted): String =
            arbeidssted.identer
                .first {
                    it.type == IdentType.ORGANISASJONSNUMMER
                }.ident

        fun getJuridiskOrgnummerFraOpplysningspliktig(opplysningspliktig: Opplysningspliktig): String =
            opplysningspliktig.identer
                .first {
                    it.type == IdentType.ORGANISASJONSNUMMER
                }.ident

        fun parseArbeidsforholdType(kode: String): ArbeidsforholdType =
            when (kode) {
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
}
