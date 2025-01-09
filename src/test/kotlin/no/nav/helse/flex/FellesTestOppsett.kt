package no.nav.helse.flex

import no.nav.helse.flex.arbeidsforhold.ArbeidsforholdRepository
import no.nav.helse.flex.narmesteleder.NARMESTELEDER_LEESAH_TOPIC
import no.nav.helse.flex.narmesteleder.NarmesteLederRepository
import no.nav.helse.flex.narmesteleder.domain.NarmesteLederLeesah
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.util.UUID

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

    @Autowired
    lateinit var kafkaProducer: Producer<String, String>

    @Autowired
    lateinit var narmesteLederRepository: NarmesteLederRepository

    @Autowired
    lateinit var arbeidsforholdRepository: ArbeidsforholdRepository

    companion object {
        init {

            KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1")).apply {
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

            startMockWebServere()
        }
    }

    @AfterAll
    fun `Vi resetter databasen`() {
        slettDatabase()
    }

    fun slettDatabase() {
        narmesteLederRepository.deleteAll()
        arbeidsforholdRepository.deleteAll()
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

    fun sendNarmesteLederLeesah(nl: NarmesteLederLeesah) {
        kafkaProducer.send(
            ProducerRecord(
                NARMESTELEDER_LEESAH_TOPIC,
                null,
                nl.narmesteLederId.toString(),
                nl.serialisertTilString(),
            ),
        ).get()
    }
}
