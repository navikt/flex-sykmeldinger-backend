package no.nav.helse.flex.mockdispatcher

import no.nav.helse.flex.pdl.*
import okhttp3.Headers
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType

object PdlMockDispatcher : Dispatcher() {
    data class InterceptedRequest(
        val headers: Headers,
        val body: String,
    )

    val requester = mutableListOf<InterceptedRequest>()

    override fun dispatch(request: RecordedRequest): MockResponse {
        val content = request.body.readUtf8()
        val graphReq: GraphQlRequest = GraphQlRequest.fraJson(content)
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

    fun skapResponse(
        fornavn: String,
        etternavn: String,
    ): MockResponse {
        val hentNavnResponse: GraphQlResponse<GetPersonResponseData> =
            GraphQlResponse(
                errors = emptyList(),
                data =
                    GetPersonResponseData(
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
            .setBody(hentNavnResponse.tilJson())
            .setHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    }
}
