package no.nav.helse.flex.config

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
import java.net.URI
import java.time.Duration

@Configuration
@EnableCaching
class CacheConfigValkey(
    @Value("\${VALKEY_URI_SESSIONS}") val valkeyUriString: String,
    @Value("\${VALKEY_USERNAME_SESSIONS}") val valkeyUsername: String,
    @Value("\${VALKEY_PASSWORD_SESSIONS}") val valkeyPassword: String,
) {
    @Bean
    fun valkeyConnectionFactory(): LettuceConnectionFactory {
        val valkeyUri = URI.create(valkeyUriString)
        val valkeyConnection = RedisStandaloneConfiguration(valkeyUri.host, valkeyUri.port)

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

        return LettuceConnectionFactory(valkeyConnection, clientConfiguration)
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
