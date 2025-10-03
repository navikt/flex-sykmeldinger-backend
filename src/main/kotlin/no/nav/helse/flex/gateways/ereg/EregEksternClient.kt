package no.nav.helse.flex.gateways.ereg

import no.nav.helse.flex.utils.logger
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Component
class EregEksternClient(
    private val eregRestClient: RestClient,
) : EregClient {
    val log = logger()

    @Retryable
    override fun hentNokkelinfo(orgnummer: String): Nokkelinfo {
        val uri = eregRestClient.get().uri { uriBuilder -> uriBuilder.path("/v2/organisasjon/$orgnummer/noekkelinfo").build() }
        val res = uri.retrieve().toEntity<Nokkelinfo>().body

        return res ?: throw RuntimeException("hentNokkelinfo response inneholdt ikke data")
    }
}
