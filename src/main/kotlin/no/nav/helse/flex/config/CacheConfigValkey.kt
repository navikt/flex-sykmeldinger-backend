package no.nav.helse.flex.config

import no.nav.helse.flex.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfigValkey(
    @Value("\${VALKEY_HOST_SESSIONS}") val valkeyHost: String,
    @Value("\${VALKEY_PORT_SESSIONS}") val valkeyPort: Int,
    @Value("\${VALKEY_USERNAME_SESSIONS}") val valkeyUsername: String,
    @Value("\${VALKEY_PASSWORD_SESSIONS}") val valkeyPassword: String,
) {
    private val log = logger()

    @Bean
    fun valkeyConnectionFactory(): LettuceConnectionFactory =
        try {
            val valkeyConnection = RedisStandaloneConfiguration(valkeyHost, valkeyPort)

            valkeyConnection.username = valkeyUsername
            valkeyConnection.password = RedisPassword.of(valkeyPassword)

            val clientConfiguration =
                LettuceClientConfiguration
                    .builder()
                    .apply {
                        if ("default" != valkeyUsername) {
                            useSsl()
                        }
                    }.build()

            LettuceConnectionFactory(valkeyConnection, clientConfiguration)
        } catch (e: Exception) {
            log.error("Error creating Valkey connection factory", e)
            throw e
        }

    @Bean
    fun cacheManager(valkeyConnectionFactory: RedisConnectionFactory): CacheManager {
        val cacheConfigurations: MutableMap<String, RedisCacheConfiguration> = HashMap()

        cacheConfigurations["flex-folkeregister-identer-med-historikk"] =
            RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))

        return RedisCacheManager
            .builder(valkeyConnectionFactory)
            .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
            .withInitialCacheConfigurations(cacheConfigurations)
            .enableStatistics()
            .build()
    }
}
