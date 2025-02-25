package no.nav.helse.flex.testconfig

import no.nav.helse.flex.config.RestClientConfig
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.client.RestClient

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnableMockOAuth2Server
@EnableJwtTokenValidation
@EnableOAuth2Client
@SpringBootTest(classes = [RestClient::class, RestClientConfig::class, MockWebServereConfig::class])
annotation class RestClientOppsett
