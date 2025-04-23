package no.nav.helse.flex.clients.pdl

import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.time.LocalDate
import java.util.*

private const val HEADER_TEMA = "Tema"
private const val TEMA_SYK = "SYK"
private const val HEADER_BEHANDLINGSNUMMER = "Behandlingsnummer"

// Se behandlinger: https://behandlingskatalog.ansatt.nav.no/process/purpose/SYKEPENGER
private const val BEHANDLINGSKODE_MOTTA_OG_BEHANDLE_SYKMELDING = "B229"

@Component
class PdlEksternClient(
    pdlRestClient: RestClient,
) : PdlClient {
    private val pdlGraphQlCLient = PdlGraphQlClient(pdlRestClient)

    @Retryable(exclude = [FunctionalPdlError::class])
    override fun hentIdenterMedHistorikk(ident: String): List<PdlIdent> {
        val response: GraphQlResponse<HentIdenterResponseData> =
            pdlGraphQlCLient.exchange(
                req =
                    GraphQlRequest(
                        query =
                            """
                            query(${"$"}ident: ID!) {
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
                        HEADER_TEMA to TEMA_SYK,
                        HEADER_BEHANDLINGSNUMMER to BEHANDLINGSKODE_MOTTA_OG_BEHANDLE_SYKMELDING,
                    ),
            )

        val data =
            response.data
                ?: throw FunctionalPdlError("Fant ikke person, ingen body eller data. ${response.errorsTilString()}")

        val identer = data.hentIdenter?.identer ?: emptyList()
        return identer
    }

    @Retryable(exclude = [FunctionalPdlError::class])
    override fun hentFormattertNavn(fnr: String): String {
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
                        HEADER_TEMA to TEMA_SYK,
                        HEADER_BEHANDLINGSNUMMER to BEHANDLINGSKODE_MOTTA_OG_BEHANDLE_SYKMELDING,
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

    @Retryable(exclude = [FunctionalPdlError::class])
    override fun hentFoedselsdato(fnr: String): LocalDate {
        val response: GraphQlResponse<GetPersonResponseData> =
            pdlGraphQlCLient.exchange(
                req =
                    GraphQlRequest(
                        query =
                            """
                            query(${"$"}ident: ID!) {
                              hentPerson(ident: ${"$"}ident) {
                                foedselsdato {
                                  foedselsdato
                                }
                              }
                            }
                            """.trimIndent(),
                        variables = Collections.singletonMap("ident", fnr),
                    ),
                headers =
                    mapOf(
                        HEADER_TEMA to TEMA_SYK,
                        HEADER_BEHANDLINGSNUMMER to BEHANDLINGSKODE_MOTTA_OG_BEHANDLE_SYKMELDING,
                    ),
            )

        val data =
            response.data
                ?: throw FunctionalPdlError("Fant ikke person, ingen body eller data. ${response.errorsTilString()}")

        val foedselsdato =
            data.hentPerson
                ?.foedselsdato
                ?.firstOrNull()
                ?.tilLocalDate()
                ?: throw PdlManglerFoedselsdatoError("Fant ikke fødselsdato i pdl response. ${response.errorsTilString()}")
        return foedselsdato
    }
}
