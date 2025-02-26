package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.clients.ereg.EregClient
import no.nav.helse.flex.clients.ereg.Navn
import no.nav.helse.flex.clients.ereg.Nokkelinfo

class EregClientFake : EregClient {
    private val nokkelinfo: MutableMap<String, Result<Nokkelinfo>> = mutableMapOf()

    companion object {
        val defaultNokkelinfo = Nokkelinfo(Navn("Org Navn"))
    }

    fun reset() {
        nokkelinfo.clear()
    }

    fun setNokkelinfo(
        nokkelinfo: Nokkelinfo,
        orgnummer: String = "__accept_any_orgnummer",
    ) {
        this.nokkelinfo[orgnummer] = Result.success(nokkelinfo)
    }

    fun setNokkelinfo(
        failure: Exception,
        orgnummer: String = "__accept_any_orgnummer",
    ) {
        this.nokkelinfo[orgnummer] = Result.failure(failure)
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
}
