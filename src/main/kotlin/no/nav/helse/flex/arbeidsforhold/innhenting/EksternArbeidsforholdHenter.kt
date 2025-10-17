package no.nav.helse.flex.arbeidsforhold.innhenting

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdType
import no.nav.helse.flex.config.EnvironmentToggles
import no.nav.helse.flex.config.PersonIdenter
import no.nav.helse.flex.gateways.aareg.AaregClient
import no.nav.helse.flex.gateways.aareg.ArbeidsforholdoversiktResponse
import no.nav.helse.flex.gateways.aareg.ArbeidsstedType
import no.nav.helse.flex.gateways.ereg.EregClient
import no.nav.helse.flex.utils.errorSecure
import no.nav.helse.flex.utils.logger
import org.springframework.stereotype.Component
import org.springframework.web.client.ResourceAccessException
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
    private val environmentToggles: EnvironmentToggles,
) {
    private val log = logger()

    fun hentEksterneArbeidsforholdForPerson(fnr: String): IdenterOgEksterneArbeidsforhold {
        val result =
            try {
                aaregClient.getArbeidsforholdoversikt(fnr)
            } catch (e: ResourceAccessException) {
                log.error("Klarte ikke hente Aareg arbeidsforhold: ${e.message}", e)
                throw e
            } catch (e: Exception) {
                if (environmentToggles.isDevelopment()) {
                    log.warn("AAREG er midlertidig nede i dev. Returnerer tom liste.", e)
                    ArbeidsforholdoversiktResponse(emptyList())
                } else {
                    log.errorSecure("Feil ved getArbeidsforholdoversikt i AaregClient", secureThrowable = e)
                    throw e
                }
            }

        val eksterneArbeidsforhold =
            result.arbeidsforholdoversikter
                .filter { it.arbeidssted.type == ArbeidsstedType.Underenhet }
                .map { arbeidsforholdOversikt ->
                    val orgnummer = arbeidsforholdOversikt.arbeidssted.finnOrgnummer()
                    val orgNokkelinfo = eregClient.hentNokkelinfo(orgnummer)
                    val orgnavn = orgNokkelinfo.navn.sammensattnavn
                    EksterntArbeidsforhold(
                        navArbeidsforholdId = arbeidsforholdOversikt.navArbeidsforholdId,
                        orgnummer = orgnummer,
                        juridiskOrgnummer = arbeidsforholdOversikt.opplysningspliktig.finnOrgnummer(),
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
