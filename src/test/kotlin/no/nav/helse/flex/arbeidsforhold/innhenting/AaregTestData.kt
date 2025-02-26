package no.nav.helse.flex.arbeidsforhold.innhenting

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.clients.aareg.ArbeidsforholdOversikt
import no.nav.helse.flex.clients.aareg.ArbeidsforholdoversiktResponse
import no.nav.helse.flex.utils.objectMapper

fun lagArbeidsforholdOversiktResponse(
    arbeidsforholdoversikter: List<ArbeidsforholdOversikt> = listOf(lagArbeidsforholdOversikt()),
): ArbeidsforholdoversiktResponse =
    ArbeidsforholdoversiktResponse(
        arbeidsforholdoversikter = arbeidsforholdoversikter,
    )

fun lagArbeidsforholdOversikt(
    identer: List<String> = listOf("2175141353812"),
    orgnummer: String = "910825518",
    navArbeidsforholdId: String = "navArbeidsforholdId",
): ArbeidsforholdOversikt =
    objectMapper.readValue(
        """
            {
          "type": {
            "kode": "ordinaertArbeidsforhold",
            "beskrivelse": "Ordinært arbeidsforhold"
          },
          "arbeidstaker": {
            "identer": [
            ${
            identer.joinToString(",\n") {
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
                "ident": "$orgnummer"
              }
            ]
          },
          "opplysningspliktig": {
            "type": "Hovedenhet",
            "identer": [
              {
                "type": "ORGANISASJONSNUMMER",
                "ident": "810825472"
              }
            ]
          },
          "startdato": "2014-01-01",
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
