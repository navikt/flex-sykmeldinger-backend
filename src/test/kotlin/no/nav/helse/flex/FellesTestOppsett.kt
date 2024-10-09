package no.nav.helse.flex

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.util.*

private class PostgreSQLContainer14 : PostgreSQLContainer<PostgreSQLContainer14>("postgres:14-alpine")

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
abstract class FellesTestOppsett {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var server: MockOAuth2Server

    companion object {
        init {

            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1")).apply {
                start()
                System.setProperty("KAFKA_BROKERS", bootstrapServers)
            }

            PostgreSQLContainer14().apply {
                withCommand("postgres", "-c", "wal_level=logical")
                start()
                System.setProperty("spring.datasource.url", "$jdbcUrl&reWriteBatchedInserts=true")
                System.setProperty("spring.datasource.username", username)
                System.setProperty("spring.datasource.password", password)
            }
        }
    }

    @AfterAll
    fun `Vi resetter databasen`() {
    }

    fun tokenxToken(
        fnr: String,
        acrClaim: String = "Level4",
        audience: String = "flex-sykmeldinger-backend-client-id",
        issuerId: String = "tokenx",
        clientId: String = "frontend-client-id",
        claims: Map<String, Any> =
            mapOf(
                "acr" to acrClaim,
                "idp" to "idporten",
                "client_id" to clientId,
                "pid" to fnr,
            ),
    ): String {
        return server.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = UUID.randomUUID().toString(),
                audience = listOf(audience),
                claims = claims,
                expiry = 3600,
            ),
        ).serialize()
    }
}
