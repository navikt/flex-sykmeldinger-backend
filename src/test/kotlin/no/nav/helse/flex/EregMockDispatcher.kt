package no.nav.helse.flex

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest

object EregMockDispatcher : Dispatcher() {
    override fun dispatch(request: RecordedRequest): MockResponse {
        return MockResponse()
            .addHeader("Content-Type", "application/json")
            .setBody(EKSEMPEL_RESPONSE_FRA_EREG.serialisertTilString())
    }
}

private val EKSEMPEL_RESPONSE_FRA_EREG =
    objectMapper.readTree(
        """
    {
  "organisasjonsnummer": "990983666",
  "navn": {
    "sammensattnavn": "NAV FAMILIE- OG PENSJONSYTELSER OSL",
    "navnelinje1": "NAV FAMILIE- OG PENSJONSYTELSER",
    "navnelinje2": "OSL",
    "navnelinje3": "string",
    "navnelinje4": "string",
    "navnelinje5": "string",
    "bruksperiode": {
      "fom": "2015-01-06T21:44:04.748",
      "tom": "2015-12-06T19:45:04"
    },
    "gyldighetsperiode": {
      "fom": "2014-07-01",
      "tom": "2015-12-31"
    }
  },
  "enhetstype": "BEDR",
  "adresse": {
    "adresselinje1": "string",
    "adresselinje2": "string",
    "adresselinje3": "string",
    "postnummer": "0557",
    "poststed": "string",
    "landkode": "JPN",
    "kommunenummer": "0301",
    "bruksperiode": {
      "fom": "2015-01-06T21:44:04.748",
      "tom": "2015-12-06T19:45:04"
    },
    "gyldighetsperiode": {
      "fom": "2014-07-01",
      "tom": "2015-12-31"
    },
    "type": "string"
  },
  "opphoersdato": "2016-12-31"
}
""",
    )
