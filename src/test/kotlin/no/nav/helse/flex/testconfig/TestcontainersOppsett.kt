package no.nav.helse.flex.testconfig

import no.nav.helse.flex.utils.logger
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName

// TODO: Bytt fra latest til en konkret versjon når det er tilgjengelig i dockerhub
private class ValkeyContainer : GenericContainer<ValkeyContainer>("bitnamisecure/valkey:latest")

private class PostgreSQLContainer14 : PostgreSQLContainer<PostgreSQLContainer14>("postgres:14-alpine")

object TestcontainersOppsett {
    private val logger = logger()

    private val kafkaContainer =
        KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.1")).apply {
            start()
            System.setProperty("KAFKA_BROKERS", bootstrapServers)

            logger.info(
                "Started kafka testcontainer: bootstrapServers=${this.bootstrapServers}, " +
                    "image=${this.dockerImageName}, containerId=${this.containerId.take(12)}",
            )
        }

    private val postgresContainer =
        PostgreSQLContainer14().apply {
            withCommand("postgres", "-c", "wal_level=logical")
            start()

            logger.info(
                "Started Postgres testcontainer: jdbcUrl=${this.jdbcUrl}, " +
                    "image=${this.dockerImageName}, containerId=${this.containerId.take(12)}",
            )
        }

    private val valkeyContainer =
        ValkeyContainer().apply {
            withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            withExposedPorts(6379)
            start()

            logger.info(
                "Started Velkey testcontainer: host=${this.host}, port=${this.firstMappedPort}, " +
                    "image=${this.dockerImageName}, containerId=${this.containerId.take(12)}",
            )
        }

    fun setPropertiesInContext(registry: DynamicPropertyRegistry) {
        postgresContainer.run {
            registry.add("spring.datasource.url") { "$jdbcUrl&reWriteBatchedInserts=true" }
            registry.add("spring.datasource.username") { username }
            registry.add("spring.datasource.password") { password }
        }
        kafkaContainer.run {
            registry.add("KAFKA_BROKERS") { bootstrapServers }
        }
        valkeyContainer.run {
            registry.add("VALKEY_HOST_SESSIONS") { host }
            registry.add("VALKEY_PORT_SESSIONS") { firstMappedPort.toString() }
            registry.add("VALKEY_USERNAME_SESSIONS") { "default" }
            registry.add("VALKEY_PASSWORD_SESSIONS") { "" }
        }
    }
}
