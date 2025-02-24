package no.nav.helse.flex.clients.pdl

import okhttp3.mockwebserver.MockResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

fun lagHentIdenterResponseData(identer: Iterable<PdlIdent> = emptyList()): HentIdenterResponseData =
    HentIdenterResponseData(
        hentIdenter =
            HentIdenter(
                identer = identer.toList(),
            ),
    )

fun lagGetPersonResponseData(
    fornavn: String = "Ole",
    mellomnavn: String? = null,
    etternavn: String = "Gunnar",
): GetPersonResponseData =
    GetPersonResponseData(
        hentPerson =
            HentPerson(
                navn =
                    listOf(
                        Navn(fornavn = fornavn, mellomnavn = mellomnavn, etternavn = etternavn),
                    ),
            ),
    )

fun <T : Any> lagGraphQlResponse(
    data: T,
    errors: List<ResponseError> = emptyList(),
): MockResponse {
    val response: GraphQlResponse<T> =
        GraphQlResponse(
            errors = errors,
            data = data,
        )

    return MockResponse()
        .setBody(response.tilJson())
        .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
}
