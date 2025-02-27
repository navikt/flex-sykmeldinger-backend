package no.nav.helse.flex.clients.pdl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.utils.objectMapper
import no.nav.helse.flex.utils.serialisertTilString
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import kotlin.reflect.KClass

class PdlGraphQlClient(
    private val restClient: RestClient,
) {
    inline fun <reified T : Any> exchange(
        req: GraphQlRequest,
        headers: Map<String, String>,
    ): GraphQlResponse<T> = exchange(req, headers, T::class)

    fun <T : Any> exchange(
        req: GraphQlRequest,
        headers: Map<String, String>,
        type: KClass<T>,
    ): GraphQlResponse<T> {
        val uri = restClient.post().uri { uriBuilder -> uriBuilder.path("/graphql").build() }
        val responseEntity =
            uri
                .headers { httpHeaders ->
                    httpHeaders.contentType = MediaType.APPLICATION_JSON
                    headers.forEach { (key, value) ->
                        httpHeaders.add(key, value)
                    }
                }.body(
                    try {
                        req.serialisertTilString()
                    } catch (e: JsonProcessingException) {
                        throw RuntimeException(e)
                    },
                ).retrieve()
                .onStatus(HttpStatusCode::isError) { _, response ->
                    throw RuntimeException("PDL svarer med status: ${response.statusCode}")
                }.toEntity<String>()

        val resBody = responseEntity.body
        if (resBody == null) {
            return GraphQlResponse(data = null, errors = null)
        } else {
            val parsedResponse = GraphQlResponse.fraJson(resBody, type)
            return parsedResponse
        }
    }
}

data class GraphQlRequest(
    val query: String,
    val variables: Map<String, String>,
)

data class GraphQlResponse<out T : Any>(
    val data: T?,
    val errors: List<ResponseError>?,
) {
    companion object {
        inline fun <reified T : Any> fraJson(json: String): GraphQlResponse<T> = fraJson(json, T::class)

        fun <T : Any> fraJson(
            json: String,
            clazz: KClass<T>,
        ): GraphQlResponse<T> {
            val mapper = objectMapper
            val mappedType = mapper.typeFactory.constructParametricType(GraphQlResponse::class.java, clazz.java)
            return mapper
                .readValue(json, mappedType)
        }
    }

    fun tilJson(): String = this.serialisertTilString()

    fun errorsTilString(): String? = this.errors?.map { it.message }?.joinToString(" - ")
}

data class ResponseError(
    val message: String?,
    val locations: List<ErrorLocation>?,
    val path: List<String>?,
    val extensions: ErrorExtension?,
)

data class ErrorLocation(
    val line: String?,
    val column: String?,
)

data class ErrorExtension(
    val code: String?,
    val classification: String?,
)
