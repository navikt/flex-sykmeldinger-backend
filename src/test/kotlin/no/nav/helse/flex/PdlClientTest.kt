package no.nav.helse.flex

import no.nav.helse.flex.mockdispatcher.PdlMockDispatcher
import no.nav.helse.flex.pdl.*
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldStartWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class PdlClientTest : FellesTestOppsett() {
    @Autowired
    private lateinit var pdlClient: PdlClient

    final val fnr = "12345678910"

    @Test
    fun `Vi tester happycase`() {
        

        val responseData = pdlClient.hentFormattertNavn(fnr)

        responseData `should be equal to` "Ole Gunnar"

        val request = PdlMockDispatcher.requester.last()
        request.headers["Behandlingsnummer"] `should be equal to` "B128"
        request.headers["Tema"] `should be equal to` "SYK"
        request.body `should be equal to`
            "{\"query\":\"\\nquery(\$ident: ID!)" +
            "{\\n  hentPerson(ident: \$ident) " +
            "{\\n  \\tnavn(historikk: false) " +
            "{\\n  \\t  fornavn\\n  \\t  mellomnavn\\n  \\t  etternavn\\n    }" +
            "\\n  }\\n}\\n\",\"variables\":{\"ident\":\"12345678910\"}}"

        request.headers["Authorization"]!!.shouldStartWith("Bearer ey")
    }

    @Test
    fun `Tor-Henry blir riktig kapitalisert`() {

        val responseData = pdlClient.hentFormattertNavn("00888888821")
        responseData `should be equal to` "Tor-Henry Roarsen"
    }

    @Test
    fun `æøå blir riktig`() {

        val responseData = pdlClient.hentFormattertNavn("00333888821")
        responseData `should be equal to` "Åge Roger Åæøå"
    }
}
