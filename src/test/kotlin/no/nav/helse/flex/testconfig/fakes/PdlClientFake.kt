package no.nav.helse.flex.testconfig.fakes

import no.nav.helse.flex.clients.pdl.PdlClient
import no.nav.helse.flex.clients.pdl.PdlIdent

class PdlClientFake : PdlClient {
    private val identerMedHistorikk: MutableMap<String, Result<List<PdlIdent>>> = mutableMapOf()
    private val formaterteNavn: MutableMap<String, Result<String>> = mutableMapOf()

    init {
        reset()
    }

    companion object {
        val defaultIdenterMedHistorikk = listOf(PdlIdent(gruppe = "default-gruppe", ident = "default-ident"))
        val defaultFormatertNavn = "default-navn"
    }

    fun setIdentMedHistorikk(
        identer: List<PdlIdent>,
        ident: String = "__default",
    ) {
        this.identerMedHistorikk[ident] = Result.success(identer)
    }

    fun setIdentMedHistorikk(
        failure: Exception,
        ident: String = "__default",
    ) {
        this.identerMedHistorikk[ident] = Result.failure(failure)
    }

    fun setFormatertNavn(
        navn: String,
        fnr: String = "__default",
    ) {
        this.formaterteNavn[fnr] = Result.success(navn)
    }

    fun setFormatertNavn(
        failure: Exception,
        fnr: String = "__default",
    ) {
        this.formaterteNavn[fnr] = Result.failure(failure)
    }

    fun reset() {
        identerMedHistorikk.clear()
        setIdentMedHistorikk(defaultIdenterMedHistorikk)
        formaterteNavn.clear()
        setFormatertNavn(defaultFormatertNavn)
    }

    override fun hentIdenterMedHistorikk(ident: String): List<PdlIdent> {
        val identHistorikk = identerMedHistorikk[ident] ?: identerMedHistorikk["__default"]!!
        return identHistorikk.getOrThrow()
    }

    override fun hentFormattertNavn(fnr: String): String {
        val formattertNavn = formaterteNavn[fnr] ?: formaterteNavn["__default"]!!
        return formattertNavn.getOrThrow()
    }
}
