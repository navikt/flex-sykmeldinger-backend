package no.nav.helse.flex.config

import no.nav.helse.flex.clients.pdl.lagGraphQlResponse
import no.nav.helse.flex.clients.pdl.lagHentIdenterResponseData
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.defaultPdlDispatcher
import no.nav.helse.flex.testconfig.simpleDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be instance of`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.data.redis.cache.FixedDurationTtlFunction
import org.springframework.data.redis.cache.RedisCache
import java.time.Duration

class IdentServiceIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var identService: IdentService

    @Autowired
    lateinit var cacheManager: CacheManager

    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

    @AfterEach
    fun setup() {
        cacheManager.getCache("flex-folkeregister-identer-med-historikk")?.clear()
        pdlMockWebServer.dispatcher = defaultPdlDispatcher
    }

    @Test
    fun `burde bruke cache`() {
        var pdlCalls = 0
        pdlMockWebServer.dispatcher =
            simpleDispatcher {
                pdlCalls++
                lagGraphQlResponse(lagHentIdenterResponseData())
            }
        identService.hentFolkeregisterIdenterMedHistorikkForFnr("ny-ident")
        identService.hentFolkeregisterIdenterMedHistorikkForFnr("ny-ident")
        identService.hentFolkeregisterIdenterMedHistorikkForFnr("ny-ident")

        pdlCalls `should be equal to` 1
    }

    @Test
    fun `burde sjekke at TTL i cache er satt rikitg`() {
        val cache = cacheManager.getCache("flex-folkeregister-identer-med-historikk") as? RedisCache
        cache `should be instance of` RedisCache::class

        val ttlFunction = cache?.cacheConfiguration?.ttlFunction as? FixedDurationTtlFunction
        ttlFunction `should be instance of` FixedDurationTtlFunction::class

        ttlFunction?.duration `should be equal to` Duration.ofHours(1)
    }
}
