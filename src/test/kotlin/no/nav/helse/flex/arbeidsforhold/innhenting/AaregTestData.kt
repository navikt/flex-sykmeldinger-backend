package no.nav.helse.flex.arbeidsforhold.innhenting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.gateways.aareg.ArbeidsforholdOversikt
import no.nav.helse.flex.gateways.aareg.ArbeidsforholdoversiktResponse
import no.nav.helse.flex.utils.objectMapper
import java.time.LocalDate

fun lagArbeidsforholdOversiktResponse(
    arbeidsforholdoversikter: List<ArbeidsforholdOversikt> = listOf(lagArbeidsforholdOversikt()),
): ArbeidsforholdoversiktResponse =
    ArbeidsforholdoversiktResponse(
        arbeidsforholdoversikter = arbeidsforholdoversikter,
    )

fun lagArbeidsforholdOversikt(
    navArbeidsforholdId: String = "navArbeidsforholdId",
    typeKode: String = "ordinaertArbeidsforhold",
    arbeidstakerIdenter: List<String> = listOf("2175141353812"),
    arbeidsstedOrgnummer: String = "910825518",
    opplysningspliktigOrgnummer: String = "810825472",
    startdato: LocalDate = LocalDate.parse("2014-01-01"),
    sluttdato: LocalDate? = null,
): ArbeidsforholdOversikt =
    objectMapper.readValue(
        """
            {
          "type": {
            "kode": "$typeKode",
            "beskrivelse": "Ordinært arbeidsforhold"
          },
          "arbeidstaker": {
            "identer": [
            ${
            arbeidstakerIdenter.joinToString(",\n") {
                """
                {
                  "type": "FOLKEREGISTERIDENT",
                  "ident": "$it",
                  "gjeldende": true
                }
                """
            }
        }
            ]
          },
          "arbeidssted": {
            "type": "Underenhet",
            "identer": [
              {
                "type": "ORGANISASJONSNUMMER",
                "ident": "$arbeidsstedOrgnummer"
              }
            ]
          },
          "opplysningspliktig": {
            "type": "Hovedenhet",
            "identer": [
              {
                "type": "ORGANISASJONSNUMMER",
                "ident": "$opplysningspliktigOrgnummer"
              }
            ]
          },
          "startdato": "$startdato",
          "sluttdato": ${sluttdato?.let { "\"$it\"" }},
          "yrke": {
            "kode": "1231119",
            "beskrivelse": "KONTORLEDER"
          },
          "avtaltStillingsprosent": 100,
          "permisjonsprosent": 50,
          "permitteringsprosent": 50,
          "rapporteringsordning": {
            "kode": "a-ordningen",
            "beskrivelse": "Rapportert via a-ordningen (2015-d.d.)"
          },
          "navArbeidsforholdId": "$navArbeidsforholdId",
          "sistBekreftet": "2020-09-15T08:19:53"
        },
        """.trimIndent(),
    )
