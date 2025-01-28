package no.nav.helse.flex.pdl

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.objectMapper
import no.nav.helse.flex.serialisertTilString
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import kotlin.reflect.KClass

class PdlGraphQlClient(
    private val url: String,
    private val restTemplate: RestTemplate,
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
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                headers.forEach { (key, value) -> this[key] = value }
            }

        val responseEntity =
            restTemplate.exchange(
                url,
                HttpMethod.POST,
                HttpEntity(
                    try {
                        req.tilJson()
                    } catch (e: JsonProcessingException) {
                        throw RuntimeException(e)
                    },
                    headers,
                ),
                String::class.java,
            )

        if (responseEntity.statusCode != HttpStatus.OK) {
            throw RuntimeException("PDL svarer med status ${responseEntity.statusCode} - ${responseEntity.body}")
        }

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
) {
    companion object {
        fun fraJson(json: String): GraphQlRequest = objectMapper.readValue(json)
    }

    fun tilJson(): String = ObjectMapper().writeValueAsString(this)
}

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
