package no.nav.helse.flex

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

class ApplicationTest : FellesTestOppsett() {
    @Test
    fun contextLoads() {
        mockMvc.perform(MockMvcRequestBuilders.get("/internal/health"))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value("UP"))
    }
}
