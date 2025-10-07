package no.nav.helse.flex

import no.nav.helse.flex.testconfig.*
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestContextManager
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureObservability
@EnableMockOAuth2Server
@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc(print = MockMvcPrint.NONE, printOnlyOnFailure = false)
@ActiveProfiles("default")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class ApplicationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @AfterAll
    fun dirtiesContextAfterClass() {
        markContextDirty()
    }

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            TestcontainersOppsett.setPropertiesInContext(registry)
        }
    }

    @Test
    fun contextLoads() {
        mockMvc
            .perform(MockMvcRequestBuilders.get("/internal/health"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }
}

private fun Any.markContextDirty() {
    TestContextManager(this::class.java)
        .testContext
        .markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE)
}
