package no.nav.helse.flex.testconfig.fakes

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("fakes")
@Configuration
@EnableCaching
class CacheConfigFake {
    @Bean
    fun cacheManager(): CacheManager =
        ConcurrentMapCacheManager(
            "flex-folkeregister-identer-med-historikk",
        )
}
