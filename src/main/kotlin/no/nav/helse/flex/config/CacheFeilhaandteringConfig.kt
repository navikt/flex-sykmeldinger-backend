package no.nav.helse.flex.config

import no.nav.helse.flex.utils.logger
import org.springframework.cache.Cache
import org.springframework.cache.annotation.CachingConfigurer
import org.springframework.cache.interceptor.CacheErrorHandler
import org.springframework.context.annotation.Configuration

@Configuration
class CacheFeilhaandteringConfig : CachingConfigurer {
    private val logger = logger()

    override fun errorHandler(): CacheErrorHandler =
        object : CacheErrorHandler {
            override fun handleCacheGetError(
                exception: RuntimeException,
                cache: Cache,
                key: Any,
            ) {
                loggFeil("lese fra", cache, exception)
            }

            override fun handleCachePutError(
                exception: RuntimeException,
                cache: Cache,
                key: Any,
                value: Any?,
            ) {
                loggFeil("skrive til", cache, exception)
            }

            override fun handleCacheEvictError(
                exception: RuntimeException,
                cache: Cache,
                key: Any,
            ) {
                loggFeil("slette fra", cache, exception)
            }

            override fun handleCacheClearError(
                exception: RuntimeException,
                cache: Cache,
            ) {
                loggFeil("tømme", cache, exception)
            }
        }

    private fun loggFeil(
        operasjon: String,
        cache: Cache,
        exception: RuntimeException,
    ) {
        logger.warn(
            "Kunne ikke {} cache '{}': {} (rotårsak: {})",
            operasjon,
            cache.name,
            exception::class.simpleName,
            rotaarsak(exception)::class.simpleName,
        )
    }

    private fun rotaarsak(exception: Throwable): Throwable = generateSequence(exception) { it.cause }.last()
}
