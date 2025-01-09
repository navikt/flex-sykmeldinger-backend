package no.nav.helse.flex.mockdispatcher

import com.fasterxml.jackson.module.kotlin.readValue
// import no.nav.helse.flex.client.pdl.AKTORID
// import no.nav.helse.flex.client.pdl.FOLKEREGISTERIDENT
// import no.nav.helse.flex.client.pdl.GetPersonResponse
// import no.nav.helse.flex.client.pdl.HentIdenter
// import no.nav.helse.flex.client.pdl.PdlClient
// import no.nav.helse.flex.client.pdl.PdlIdent
// import no.nav.helse.flex.client.pdl.ResponseData
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.pdl.*
import no.nav.helse.flex.serialisertTilString
import okhttp3.Headers
// import no.nav.helse.flex.util.objectMapper
// import no.nav.helse.flex.util.serialisertTilString
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType

object PdlMockDispatcher : Dispatcher() {

data class InterceptedRequest(val headers: Headers, val body: String)
val requester = mutableListOf<InterceptedRequest>()

    override fun dispatch(request: RecordedRequest): MockResponse {
        val content = request.body.readUtf8()
        val graphReq: PdlClient.GraphQLRequest = objectMapper.readValue(content)
        val ident = graphReq.variables["ident"] ?: return MockResponse().setStatus("400").setBody("Ingen ident variabel")
        requester.add(InterceptedRequest(request.headers, content))

        if (ident.startsWith("01985554321")) {
            return skapResponse("SUPREME", "LEADER")
        }

        if (ident.startsWith("00888888821")) {
            return skapResponse("TOR-HENRY", "ROARSEN")
        }

        if (ident.startsWith("00333888821")) {
            return skapResponse("ÅGE ROGER", "ÅÆØÅ")
        }



        return skapResponse("OLE", "GUNNAR")
    }

    fun skapResponse( fornavn: String, etternavn: String): MockResponse {
        val hentNavnResponse: GetPersonResponse =
            GetPersonResponse(
                errors = emptyList(),
                data =
                    ResponseData(
                        hentPerson =
                            HentPerson(
                                navn =
                                    listOf(
                                        Navn(fornavn = fornavn, mellomnavn = null, etternavn = etternavn),
                                    ),
                            ),
                    ),
            )

        return MockResponse()
            .setBody(hentNavnResponse.serialisertTilString())
            .setHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }
}
