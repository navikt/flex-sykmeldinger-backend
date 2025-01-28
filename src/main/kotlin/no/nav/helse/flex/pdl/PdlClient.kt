package no.nav.helse.flex.pdl

import org.springframework.beans.factory.annotation.Value
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.Collections

private const val TEMA = "Tema"
private const val TEMA_SYK = "SYK"
private const val BEHANDLINGSNUMMER_KEY = "Behandlingsnummer"
private const val BEHANDLINGSNUMMER_VALUE = "B128"

@Component
class PdlClient(
    @Value("\${PDL_BASE_URL}")
    pdlApiUrl: String,
    pdlRestTemplate: RestTemplate,
) {
    private val pdlGraphQlCLient = PdlGraphQlClient("$pdlApiUrl/graphql", pdlRestTemplate)

    @Retryable(exclude = [FunctionalPdlError::class])
    fun hentIdenterMedHistorikk(ident: String): List<PdlIdent> {
        val response: GraphQlResponse<HentIdenterResponseData> =
            pdlGraphQlCLient.exchange(
                req =
                    GraphQlRequest(
                        query =
                            """
                            query(${"$"}ident: ID!){
                              hentIdenter(ident: ${"$"}ident, historikk: true) {
                                identer {
                                  ident,
                                  gruppe
                                }
                              }
                            }
                            """.trimIndent(),
                        variables = Collections.singletonMap("ident", ident),
                    ),
                headers =
                    mapOf(
                        TEMA to TEMA_SYK,
                    ),
            )

        val data =
            response.data
                ?: throw FunctionalPdlError("Fant ikke person, ingen body eller data. ${response.errorsTilString()}")

        val identer = data.hentIdenter?.identer ?: emptyList()
        return identer
    }

    @Retryable(exclude = [FunctionalPdlError::class])
    fun hentFormattertNavn(fnr: String): String {
        val response: GraphQlResponse<GetPersonResponseData> =
            pdlGraphQlCLient.exchange(
                req =
                    GraphQlRequest(
                        query =
                            """
                            query(${"$"}ident: ID!) {
                              hentPerson(ident: ${"$"}ident) {
                                navn(historikk: false) {
                                  fornavn
                                  mellomnavn
                                  etternavn
                                }
                              }
                            }
                            """.trimIndent(),
                        variables = Collections.singletonMap("ident", fnr),
                    ),
                headers =
                    mapOf(
                        TEMA to TEMA_SYK,
                        BEHANDLINGSNUMMER_KEY to BEHANDLINGSNUMMER_VALUE,
                    ),
            )

        val data =
            response.data
                ?: throw FunctionalPdlError("Fant ikke person, ingen body eller data. ${response.errorsTilString()}")

        val navn =
            data.hentPerson
                ?.navn
                ?.firstOrNull()
                ?.formatert()
                ?: throw PdlManglerNavnError("Fant ikke navn i pdl response. ${response.errorsTilString()}")
        return navn
    }
}
