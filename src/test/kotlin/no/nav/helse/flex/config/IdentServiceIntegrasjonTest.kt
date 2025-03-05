package no.nav.helse.flex.config

import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import no.nav.helse.flex.clients.pdl.PdlClient
import no.nav.helse.flex.clients.pdl.lagGraphQlResponse
import no.nav.helse.flex.clients.pdl.lagHentIdenterResponseData
import no.nav.helse.flex.testconfig.IntegrasjonTestOppsett
import no.nav.helse.flex.testconfig.simpleDispatcher
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean

class IdentServiceIntegrasjonTest : IntegrasjonTestOppsett() {
    @Autowired
    lateinit var identService: IdentService

    @Autowired
    lateinit var cacheManager: CacheManager

    @MockitoSpyBean
    lateinit var pdlEksternClient: PdlClient

    @Autowired
    lateinit var pdlMockWebServer: MockWebServer

    @BeforeEach
    fun setup() {
        cacheManager.getCache("flex-folkeregister-identer-med-historikk")?.clear()
    }

    @Test
    fun `burde bruke cache`() {
        pdlMockWebServer.dispatcher =
            simpleDispatcher {
                lagGraphQlResponse(lagHentIdenterResponseData())
            }
        identService.hentFolkeregisterIdenterMedHistorikkForFnr("ny-ident")
        identService.hentFolkeregisterIdenterMedHistorikkForFnr("ny-ident")
        identService.hentFolkeregisterIdenterMedHistorikkForFnr("ny-ident")

        verify(pdlEksternClient, times(1)).hentIdenterMedHistorikk("ny-ident")
    }
}
