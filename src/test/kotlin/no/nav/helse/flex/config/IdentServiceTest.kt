package no.nav.helse.flex.config

import no.nav.helse.flex.clients.pdl.PdlIdent
import no.nav.helse.flex.testconfig.FakesTestOppsett
import no.nav.helse.flex.testconfig.fakes.PdlClientFake
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager

class IdentServiceTest : FakesTestOppsett() {
    @Autowired
    lateinit var pdlClientFake: PdlClientFake

    @Autowired
    private lateinit var identService: IdentService

    @Autowired
    private lateinit var cacheManager: CacheManager

    @BeforeEach
    fun clearCache() {
        cacheManager.getCache("flex-folkeregister-identer-med-historikk")?.clear()
    }

    @AfterEach
    fun tearDown() {
        pdlClientFake.reset()
    }

    @Test
    fun `Burde hente historikk for fnr`() {
        pdlClientFake.setIdentMedHistorikk(listOf(PdlIdent(gruppe = "FOLKEREGISTERIDENT", ident = "pdl-ident")))

        val responseData = identService.hentFolkeregisterIdenterMedHistorikkForFnr("ny-ident")
        responseData.andreIdenter `should be equal to` listOf("pdl-ident")
        responseData.originalIdent `should be equal to` "ny-ident"
    }

    @Test
    fun `Burde kun returnere unike identer`() {
        pdlClientFake.setIdentMedHistorikk(
            listOf(
                PdlIdent(gruppe = "FOLKEREGISTERIDENT", ident = "pdl-ident"),
                PdlIdent(gruppe = "FOLKEREGISTERIDENT", ident = "ny-ident"),
            ),
        )

        val responseData = identService.hentFolkeregisterIdenterMedHistorikkForFnr("ny-ident")
        responseData.andreIdenter `should be equal to` listOf("pdl-ident")
        responseData.originalIdent `should be equal to` "ny-ident"
    }

    @Test
    fun `Burde bruke cache`() {
        pdlClientFake.setIdentMedHistorikk(
            listOf(PdlIdent(gruppe = "FOLKEREGISTERIDENT", ident = "cache-ident")),
        )

        identService.hentFolkeregisterIdenterMedHistorikkForFnr("cache-ident")
        identService.hentFolkeregisterIdenterMedHistorikkForFnr("cache-ident")
        identService.hentFolkeregisterIdenterMedHistorikkForFnr("cache-ident")

        pdlClientFake.hentIdentTeller `should be equal to` 1
    }
}
