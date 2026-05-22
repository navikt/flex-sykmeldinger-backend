package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.gateways.ereg.EregClient
import no.nav.helse.flex.gateways.ereg.HentOrganisasjonerResponse
import no.nav.helse.flex.gateways.ereg.Navn
import no.nav.helse.flex.gateways.ereg.Nokkelinfo
import no.nav.helse.flex.gateways.ereg.OrganisasjonInfo

class EregClientFake : EregClient {
    private val nokkelinfo: MutableMap<String, Result<Nokkelinfo>> = mutableMapOf()
    private var hentOrganisasjonerResponse: Result<HentOrganisasjonerResponse>? = null

    companion object {
        val defaultNokkelinfo = Nokkelinfo(Navn("Org Navn"))
    }

    fun reset() {
        nokkelinfo.clear()
        hentOrganisasjonerResponse = null
    }

    fun setHentOrganisasjonerResponse(response: HentOrganisasjonerResponse) {
        this.hentOrganisasjonerResponse = Result.success(response)
    }

    fun setHentOrganisasjonerResponse(failure: Exception) {
        this.hentOrganisasjonerResponse = Result.failure(failure)
    }

    override fun hentNokkelinfo(orgnummer: String): Nokkelinfo {
        if (nokkelinfo.isEmpty()) {
            return defaultNokkelinfo
        }
        val nokkelinfo =
            nokkelinfo[orgnummer] ?: nokkelinfo["__accept_any_orgnummer"]
                ?: throw IllegalStateException("No response found for $orgnummer")
        return nokkelinfo.getOrThrow()
    }

    override fun hentOrganisasjoner(orgnummere: List<String>): HentOrganisasjonerResponse {
        hentOrganisasjonerResponse?.let { return it.getOrThrow() }
        return HentOrganisasjonerResponse(
            organisasjoner =
                orgnummere.associateWith { orgnummer ->
                    val info = nokkelinfo[orgnummer] ?: nokkelinfo["__accept_any_orgnummer"]
                    val navn = info?.getOrThrow()?.navn ?: Navn("Org Navn")
                    OrganisasjonInfo(navn)
                },
        )
    }
}
