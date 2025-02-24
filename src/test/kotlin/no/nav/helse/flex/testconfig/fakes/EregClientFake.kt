package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.clients.ereg.EregClient
import no.nav.helse.flex.clients.ereg.Navn
import no.nav.helse.flex.clients.ereg.Nokkelinfo

class EregClientFake : EregClient {
    private val nokkelinfo: MutableMap<String, Result<Nokkelinfo>> = mutableMapOf()

    init {
        reset()
    }

    companion object {
        val defaultNokkelinfo = Nokkelinfo(Navn("Org Navn"))
    }

    fun reset() {
        nokkelinfo.clear()
        setNokkelinfo(defaultNokkelinfo)
    }

    fun setNokkelinfo(
        nokkelinfo: Nokkelinfo,
        orgnummer: String = "__default",
    ) {
        this.nokkelinfo[orgnummer] = Result.success(nokkelinfo)
    }

    fun setNokkelinfo(
        failure: Exception,
        orgnummer: String = "__default",
    ) {
        this.nokkelinfo[orgnummer] = Result.failure(failure)
    }

    override fun hentNokkelinfo(orgnummer: String): Nokkelinfo {
        val nokkelinfo = nokkelinfo[orgnummer] ?: nokkelinfo["__default"]!!
        return nokkelinfo.getOrThrow()
    }
}
