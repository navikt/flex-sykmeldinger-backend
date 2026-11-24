package no.nav.helse.flex.config

import io.kotest.matchers.shouldBe
import no.nav.helse.flex.gateways.pdl.PdlIdent
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.concurrent.ConcurrentMapCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary

@Import(CacheFeilhaandteringIntegrasjonTest.ValkeyNedeConfig::class)
class CacheFeilhaandteringIntegrasjonTest : FakesTestOppsett() {
    @Autowired
    private lateinit var identService: IdentService

    @Autowired
    private lateinit var pdlClient: PdlClientFake

    @AfterEach
    fun ryddOpp() {
        pdlClient.reset()
    }

    @Test
    fun `Burde kalle PDL hver gang når Valkey er utilgjengelig`() {
        pdlClient.setIdentMedHistorikk(
            listOf(PdlIdent(gruppe = IdentService.FOLKEREGISTERIDENT, ident = "ident-fra-pdl")),
            ident = "fnr",
        )

        repeat(2) {
            identService.hentFolkeregisterIdenterMedHistorikkForFnr("fnr") shouldBe
                PersonIdenter(originalIdent = "fnr", andreIdenter = listOf("ident-fra-pdl"))
        }

        pdlClient.antallKallTilHentIdenterMedHistorikk shouldBe 2
    }

    @TestConfiguration(proxyBeanMethods = false)
    class ValkeyNedeConfig {
        @Bean
        @Primary
        fun valkeyNedeCacheManager(): CacheManager =
            object : CacheManager {
                override fun getCache(name: String): Cache = ValkeyNedeCache(name)

                override fun getCacheNames(): Collection<String> = emptyList()
            }
    }

    private class ValkeyNedeCache(
        name: String,
    ) : ConcurrentMapCache(name) {
        override fun lookup(key: Any): Any = throw IllegalStateException("Valkey er utilgjengelig")

        override fun put(
            key: Any,
            value: Any?,
        ): Unit = throw IllegalStateException("Valkey er utilgjengelig")
    }
}
