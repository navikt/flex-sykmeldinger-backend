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

fun lagArbeidsforholdOversikt(fnr: String = "2175141353812"): ArbeidsforholdOversikt =
    objectMapper.readValue(
        """
            {
          "type": {
            "kode": "ordinaertArbeidsforhold",
            "beskrivelse": "Ordinært arbeidsforhold"
          },
          "arbeidstaker": {
            "identer": [
              {
                "type": "FOLKEREGISTERIDENT",
                "ident": "$fnr",
                "gjeldende": true
              }
            ]
          },
          "arbeidssted": {
            "type": "Underenhet",
            "identer": [
              {
                "type": "ORGANISASJONSNUMMER",
                "ident": "910825518"
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
          "navArbeidsforholdId": 12345,
          "sistBekreftet": "2020-09-15T08:19:53"
        },
        """.trimIndent(),
    )
