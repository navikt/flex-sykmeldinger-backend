import io.valkey.JedisPool
import io.valkey.JedisPoolConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ValkeyConfig(
    @Value("\${VALKEY_HOST_SESSIONS}") val valkeyHost: String,
    @Value("\${VALKEY_PORT_SESSIONS}") val valkeyPort: Int,
    @Value("\${VALKEY_USERNAME_SESSIONS}") val valkeyUsername: String,
    @Value("\${VALKEY_PASSWORD_SESSIONS}") val valkeyPassword: String,
) {
    @Bean(destroyMethod = "close")
    fun jedisPool(): JedisPool {
        val config = JedisPoolConfig()
        // Recommended settings for performance
        config.maxTotal = 32
        config.maxIdle = 32
        config.minIdle = 16

        return JedisPool(
            config,
            valkeyHost,
            valkeyPort,
            2000,
            valkeyUsername,
            valkeyPassword,
        )
    }
}
